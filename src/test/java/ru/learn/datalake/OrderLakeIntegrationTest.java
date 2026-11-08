package ru.learn.datalake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "labs.external", matches = "true")
class OrderLakeIntegrationTest {
    private static final LocalDate JAN_10 = LocalDate.parse("2025-01-10");
    private static final LocalDate JAN_11 = LocalDate.parse("2025-01-11");

    @TempDir
    Path scratch;

    private final String bucket = "lake-" + UUID.randomUUID();
    private S3Client s3;
    private OrderLake lake;
    private boolean bucketCreated;

    @BeforeEach
    void connect() {
        s3 = S3Client.builder()
                .endpointOverride(URI.create(System.getenv().getOrDefault("LAB_S3_ENDPOINT", "http://localhost:8333")))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        System.getenv().getOrDefault("LAB_S3_ACCESS_KEY", "learn"),
                        System.getenv().getOrDefault("LAB_S3_SECRET_KEY", "learn-local-secret"))))
                .forcePathStyle(true)
                .build();
        s3.createBucket(req -> req.bucket(bucket));
        bucketCreated = true;
        lake = new OrderLake(s3, bucket, scratch);
    }

    @AfterEach
    void cleanup() {
        if (s3 != null) {
            try (var ignored = s3) {
                if (bucketCreated) {
                    var keys = new LakeStorage(s3, bucket).keys("");
                    for (String key : keys) {
                        s3.deleteObject(req -> req.bucket(bucket).key(key));
                    }
                    s3.deleteBucket(req -> req.bucket(bucket));
                }
            }
        }
    }

    @Test
    void rawReplayIsIdempotentAndSilverDeduplicatesAndQuarantines() throws Exception {
        String batch = """
                {"event_id":"a","occurred_at":"2025-01-10T10:00:00Z","amount_cents":100,"revision":1}
                {"event_id":"a","occurred_at":"2025-01-10T10:00:00Z","amount_cents":100,"revision":1}
                {"event_id":"b","occurred_at":"2025-01-11T11:00:00Z","amount_cents":200,"revision":1,"channel":"web"}
                {"event_id":"c","occurred_at":"2025-01-11T11:00:00Z","amount_cents":"invalid","revision":1}
                {"event_id":"d","occurred_at":"2025-01-11T11:00:00Z","amount_cents":1.5,"revision":1}
                """;
        assertTrue(lake.ingest("batch-1", batch));
        assertFalse(lake.ingest("batch-1", batch));
        assertThrows(IllegalStateException.class, () -> lake.ingest("batch-1", batch.replace("100", "101")));

        var prepared = lake.prepare();
        assertEquals(2, prepared.rejectedRows());
        assertTrue(lake.currentSnapshot().isEmpty());
        assertTrue(lake.publish(prepared));
        assertEquals(List.of(revenue(JAN_10, 1, 100), revenue(JAN_11, 1, 200)),
                lake.revenue(prepared.id(), null));
        assertEquals(List.of(revenue(JAN_11, 1, 200)), lake.revenue(prepared.id(), JAN_11));
        var objects = new LakeStorage(s3, bucket).keys("snapshots/" + prepared.id() + "/");
        assertTrue(objects.stream().anyMatch(key -> key.contains("/event_date=2025-01-11/")));
        assertTrue(objects.stream().anyMatch(key -> key.endsWith("/quarantine.parquet")));
    }

    @Test
    void stagedFailureKeepsOldSnapshotAndLateCorrectionChangesOnlyNewView() throws Exception {
        lake.ingest("batch-1", event("a", "2025-01-10", 100, 1));
        var old = lake.prepare();
        assertTrue(lake.publish(old));

        lake.ingest("batch-2", event("a", "2025-01-10", 150, 2) + event("b", "2025-01-10", 200, 1));
        var abandoned = lake.prepare();
        // Имитируем падение после загрузки файлов до PUT указателя. Потребитель продолжает читать старый снимок.
        assertNotEquals(old.id(), abandoned.id());
        assertEquals(Optional.of(old.id()), lake.currentSnapshot());
        assertEquals(List.of(revenue(JAN_10, 1, 100)), lake.revenue(old.id(), JAN_10));

        var retry = lake.prepare();
        assertTrue(lake.publish(retry));
        assertEquals(Optional.of(retry.id()), lake.currentSnapshot());
        assertEquals(List.of(revenue(JAN_10, 2, 350)), lake.revenue(retry.id(), JAN_10));
        // Читатель с закреплённым snapshot ID не переключается во время запроса.
        assertEquals(List.of(revenue(JAN_10, 1, 100)), lake.revenue(old.id(), JAN_10));
    }

    @Test
    void staleBuilderCannotOverwriteSnapshotFromFresherInputs() throws Exception {
        lake.ingest("batch-1", event("a", "2025-01-10", 100, 1));
        var initial = lake.prepare();
        assertTrue(lake.publish(initial));

        var slow = lake.prepare();
        lake.ingest("batch-2", event("b", "2025-01-11", 200, 1));
        var fast = lake.prepare();
        assertEquals(slow.baseEtag(), fast.baseEtag());
        assertTrue(lake.publish(fast));
        // Проверяем реальный условный PUT в S3, а не блокировку внутри Java-процесса.
        assertFalse(lake.publish(slow));
        assertEquals(Optional.of(fast.id()), lake.currentSnapshot());
        assertEquals(List.of(revenue(JAN_10, 1, 100), revenue(JAN_11, 1, 200)), lake.revenue(fast.id(), null));
    }

    @Test
    void twoFirstPublishersRaceForAnEmptyCatalog() throws Exception {
        lake.ingest("batch-1", event("a", "2025-01-10", 100, 1));
        var first = lake.prepare();
        var second = lake.prepare();
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> {
                start.await();
                return lake.publish(first);
            });
            var b = pool.submit(() -> {
                start.await();
                return lake.publish(second);
            });
            start.countDown();
            boolean firstWon = a.get(10, TimeUnit.SECONDS);
            assertNotEquals(firstWon, b.get(10, TimeUnit.SECONDS));
            assertEquals(Optional.of(firstWon ? first.id() : second.id()), lake.currentSnapshot());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void conflictingPayloadForSameEventVersionDoesNotPublishCorruptSnapshot() throws Exception {
        lake.ingest("batch-1", event("a", "2025-01-10", 100, 1));
        var valid = lake.prepare();
        assertTrue(lake.publish(valid));

        lake.ingest("batch-2", event("a", "2025-01-10", 101, 1));
        var failure = assertThrows(IllegalStateException.class, lake::prepare);
        assertTrue(failure.getMessage().contains("Conflicting event revision"));
        assertEquals(Optional.of(valid.id()), lake.currentSnapshot());
    }

    private static OrderLake.DailyRevenue revenue(LocalDate date, long count, long cents) {
        return new OrderLake.DailyRevenue(date, count, cents);
    }

    private static String event(String id, String date, long amount, int revision) {
        return """
                {"event_id":"%s","occurred_at":"%sT12:00:00Z","amount_cents":%d,"revision":%d}
                """.formatted(id, date, amount, revision);
    }
}
