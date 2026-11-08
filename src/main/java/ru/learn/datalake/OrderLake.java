package ru.learn.datalake;

import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Учебный Data Lake: неизменяемый Bronze, нормализованный Silver и запрос Gold.
 * DuckDB работает с локальной временной копией файлов. Постоянные данные и каталог лежат в S3.
 */
public class OrderLake {
    private static final String CURRENT = "catalog/current";
    private final LakeStorage store;
    private final Path scratch;

    public OrderLake(S3Client client, String bucket, Path scratch) {
        this.store = new LakeStorage(client, bucket);
        this.scratch = scratch;
    }

    public record PreparedSnapshot(UUID id, String baseEtag, long rejectedRows) {
    }

    public record DailyRevenue(LocalDate day, long orders, long cents) {
    }

    public boolean ingest(String batchId, String jsonLines) {
        if (!batchId.matches("[a-zA-Z0-9_-]{1,80}")) {
            throw new IllegalArgumentException("Invalid batch id");
        }
        String key = "bronze/" + batchId + ".ndjson";
        byte[] raw = jsonLines.getBytes(UTF_8);
        if (store.compareAndSet(key, raw, null)) {
            return true;
        }
        // Повтор доставки того же файла безопасен. Изменившийся payload требует нового batch ID.
        if (store.find(key).filter(existing -> Arrays.equals(raw, existing.bytes())).isPresent()) {
            return false;
        }
        throw new IllegalStateException("Batch id already used or conflicted: " + batchId);
    }

    public PreparedSnapshot prepare() throws IOException, SQLException {
        // Запоминаем базовую версию до чтения входов. Более новый коммит не должен быть затёрт этой сборкой.
        String baseEtag = store.find(CURRENT).map(LakeStorage.ObjectVersion::etag).orElse(null);
        List<String> batches = store.keys("bronze/");
        if (batches.isEmpty()) {
            throw new IllegalStateException("No bronze batches");
        }
        UUID id = UUID.randomUUID();
        String prefix = "snapshots/" + id + "/";
        Path work = Files.createTempDirectory(scratch, "lake-build-");
        try (var connection = DriverManager.getConnection("jdbc:duckdb:");
             var sql = connection.createStatement()) {
            var inputs = new ArrayList<Path>();
            for (int i = 0; i < batches.size(); i++) {
                Path input = work.resolve("input-" + i + ".ndjson");
                store.download(batches.get(i), input);
                inputs.add(input);
            }
            sql.execute("""
                    CREATE TEMP TABLE typed AS
                    SELECT event_id, TRY_CAST(occurred_at AS TIMESTAMP) AS event_ts,
                           TRY_CAST(amount_cents AS BIGINT) AS cents, TRY_CAST(revision AS INTEGER) AS revision,
                           channel, occurred_at, amount_cents,
                           CASE
                               WHEN event_id IS NULL OR trim(event_id) = '' THEN 'missing_event_id'
                               WHEN event_ts IS NULL OR NOT regexp_full_match(occurred_at,
                                   '[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}([.][0-9]{1,6})?Z')
                                   THEN 'invalid_timestamp'
                               WHEN cents IS NULL OR NOT regexp_full_match(amount_cents, '[0-9]+') THEN 'invalid_amount'
                               WHEN TRY_CAST(revision AS INTEGER) IS NULL OR TRY_CAST(revision AS INTEGER) < 1
                                    OR NOT regexp_full_match(revision, '[0-9]+')
                                   THEN 'invalid_revision'
                           END AS reject_reason
                    FROM read_json(%s, format = 'newline_delimited', columns = {
                        event_id: 'VARCHAR', occurred_at: 'VARCHAR', amount_cents: 'VARCHAR',
                        revision: 'VARCHAR', channel: 'VARCHAR'
                    })
                    """.formatted(fileList(inputs)));
            long rejected;
            try (var rows = sql.executeQuery("SELECT count(*) FROM typed WHERE reject_reason IS NOT NULL")) {
                rows.next();
                rejected = rows.getLong(1);
            }
            // Одинаковые event_id и revision должны описывать одну версию события. Конфликт останавливает сборку.
            try (var conflicts = sql.executeQuery("""
                    SELECT event_id FROM typed WHERE reject_reason IS NULL
                    GROUP BY event_id, revision
                    HAVING count(DISTINCT struct_pack(ts := event_ts, amount := cents, source := channel)) > 1
                    LIMIT 1
                    """)) {
                if (conflicts.next()) {
                    throw new IllegalStateException("Conflicting event revision: " + conflicts.getString(1));
                }
            }
            // Пересобираем из Bronze. Поздняя запись попадёт в партицию по дате события, даже если день уже закрыт.
            Path silver = Files.createDirectory(work.resolve("silver"));
            sql.execute("""
                    COPY (
                        SELECT event_id, event_ts, cents, revision, channel, CAST(event_ts AS DATE) AS event_date
                        FROM typed WHERE reject_reason IS NULL
                        QUALIFY row_number() OVER (PARTITION BY event_id ORDER BY revision DESC) = 1
                    ) TO %s (FORMAT PARQUET, PARTITION_BY (event_date))
                    """.formatted(literal(silver.toString())));
            Path quarantine = work.resolve("quarantine.parquet");
            sql.execute("""
                    COPY (SELECT * FROM typed WHERE reject_reason IS NOT NULL) TO %s (FORMAT PARQUET)
                    """.formatted(literal(quarantine.toString())));
            store.upload(prefix + "quarantine.parquet", quarantine);

            var files = new ArrayList<String>();
            for (Path file : descendants(silver)) {
                if (Files.isRegularFile(file)) {
                    String relative = silver.relativize(file).toString().replace('\\', '/');
                    String key = prefix + "silver/" + relative;
                    store.upload(key, file);
                    files.add(key);
                }
            }
            // Manifest содержит только готовые файлы. Читатели по CURRENT ещё видят предыдущую версию.
            if (!store.compareAndSet(prefix + "manifest", String.join("\n", files).getBytes(UTF_8), null)) {
                throw new IllegalStateException("Snapshot manifest already exists: " + id);
            }
            return new PreparedSnapshot(id, baseEtag, rejected);
        } finally {
            cleanup(work);
        }
    }

    public boolean publish(PreparedSnapshot snapshot) {
        if (store.find("snapshots/" + snapshot.id() + "/manifest").isEmpty()) {
            throw new IllegalArgumentException("Snapshot has not been prepared");
        }
        // Публикация — одна условная запись указателя. Это граница видимости, а не транзакция файлов S3.
        return store.compareAndSet(CURRENT, snapshot.id().toString().getBytes(UTF_8), snapshot.baseEtag());
    }

    public Optional<UUID> currentSnapshot() {
        return store.find(CURRENT).map(version -> UUID.fromString(new String(version.bytes(), UTF_8)));
    }

    public List<DailyRevenue> revenue(UUID snapshotId, LocalDate day) throws IOException, SQLException {
        String prefix = "snapshots/" + snapshotId + "/";
        byte[] manifest = store.find(prefix + "manifest").orElseThrow().bytes();
        String selected = prefix + "silver/event_date=" + (day == null ? "" : day + "/");
        List<String> files = new String(manifest, UTF_8).lines().filter(key -> key.startsWith(selected)).toList();
        if (files.isEmpty()) {
            return List.of();
        }
        Path work = Files.createTempDirectory(scratch, "lake-read-");
        try (var connection = DriverManager.getConnection("jdbc:duckdb:");
             var sql = connection.createStatement()) {
            var downloaded = new ArrayList<Path>();
            for (String key : files) {
                // Отсекаем другие даты до GET. Hive-каталоги сохраняем, чтобы DuckDB восстановил колонку event_date.
                Path file = work.resolve(key.substring((prefix + "silver/").length())).normalize();
                if (!file.startsWith(work.normalize())) {
                    throw new IllegalStateException("Invalid manifest path: " + key);
                }
                Files.createDirectories(file.getParent());
                store.download(key, file);
                downloaded.add(file);
            }
            try (var rows = sql.executeQuery("""
                    SELECT event_date, count(*), sum(cents)::BIGINT
                    FROM read_parquet(%s, hive_partitioning = true)
                    GROUP BY event_date ORDER BY event_date
                    """.formatted(fileList(downloaded)))) {
                var result = new ArrayList<DailyRevenue>();
                while (rows.next()) {
                    result.add(new DailyRevenue(rows.getDate(1).toLocalDate(), rows.getLong(2), rows.getLong(3)));
                }
                return result;
            }
        } finally {
            cleanup(work);
        }
    }

    private static String literal(String path) {
        return "'" + path.replace('\\', '/').replace("'", "''") + "'";
    }

    private static String fileList(List<Path> files) {
        // DuckDB принимает список файлов. Windows запрещает * в Path, а manifest уже задаёт точный набор.
        return files.stream().map(path -> literal(path.toString())).collect(Collectors.joining(", ", "[", "]"));
    }

    private static List<Path> descendants(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.sorted().toList();
        }
    }

    private static void cleanup(Path work) throws IOException {
        // Сначала закрываем Files.walk, иначе Windows может удерживать открытые дескрипторы папок.
        for (Path path : descendants(work).stream().sorted(Comparator.reverseOrder()).toList()) {
            Files.deleteIfExists(path);
        }
    }
}
