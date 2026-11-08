package ru.learn.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import ru.learn.delivery.WalletTestDatabase;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static ru.learn.redis.StockReservations.Result.*;

@EnabledIfSystemProperty(named = "labs.external", matches = "true")
class RedisLabIntegrationTest {
    private final String prefix = "lab:" + UUID.randomUUID();
    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;
    private RedisCommands<String, String> redis;

    @BeforeEach
    void connect() {
        client = RedisClient.create(System.getenv().getOrDefault("LAB_REDIS_URI", "redis://localhost:6379"));
        connection = client.connect();
        redis = connection.sync();
    }

    @AfterEach
    void cleanup() {
        try {
            if (connection != null) {
                // Удаляем только известные ключи этого теста. FLUSHDB затронул бы соседние лабораторные.
                try (var ignored = connection) {
                    redis.del(prefix + ":price:sku", prefix + ":limit:alice",
                            prefix + ":lease:report", prefix + ":stock:sku", prefix + ":credits");
                }
            }
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    @Test
    void cacheHitAvoidsSourceAndInvalidationLoadsNewValue() {
        var cache = new ProductPriceCache(redis, prefix, Duration.ofSeconds(30));
        var loads = new AtomicInteger();
        assertEquals(100, cache.getPrice("sku", () -> 100 + 100L * loads.getAndIncrement()));
        assertEquals(100, cache.getPrice("sku", () -> fail("Cache hit must not call source")));
        assertTrue(redis.pttl(prefix + ":price:sku") > 0);

        cache.invalidate("sku");
        assertEquals(200, cache.getPrice("sku", () -> 100 + 100L * loads.getAndIncrement()));
        assertEquals(2, loads.get());
    }

    @Test
    void expiredCacheEntryIsLoadedAgain() {
        var cache = new ProductPriceCache(redis, prefix, Duration.ofMillis(250));
        assertEquals(100, cache.getPrice("sku", () -> 100));
        await().atMost(Duration.ofSeconds(3)).until(() -> redis.get(prefix + ":price:sku") == null);
        assertEquals(200, cache.getPrice("sku", () -> 200));
    }

    @Test
    void staleReadCanRefillCacheAfterAnotherInstanceInvalidatesIt() throws Exception {
        var price = new AtomicInteger(100);
        var readerHasOldPrice = new CountDownLatch(1);
        var finishReader = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();

        try (var peer = client.connect()) {
            var reader = new ProductPriceCache(redis, prefix, Duration.ofSeconds(30));
            var writer = new ProductPriceCache(peer.sync(), prefix, Duration.ofSeconds(30));
            var pending = executor.submit(() -> reader.getPrice("sku", () -> {
                int snapshot = price.get();
                readerHasOldPrice.countDown();
                try {
                    if (!finishReader.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Writer did not finish");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(interrupted);
                }
                return snapshot;
            }));
            assertTrue(readerHasOldPrice.await(5, TimeUnit.SECONDS));
            // Читатель уже держит старое значение, но ещё не успел записать его в Redis.
            price.set(200);
            writer.invalidate("sku");
            finishReader.countDown();

            assertEquals(100, pending.get(5, TimeUnit.SECONDS));
            assertEquals(200, price.get());
            assertEquals(100, writer.getPrice("sku", price::get));
        } finally {
            finishReader.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentCallersShareQuotaAndWindowExpires() throws Exception {
        try (var peer = client.connect()) {
            var first = new FixedWindowLimiter(redis, prefix);
            var second = new FixedWindowLimiter(peer.sync(), prefix);
            var window = Duration.ofSeconds(2);
            List<Boolean> results = parallelRequests(20,
                    index -> (index % 2 == 0 ? first : second).allow("alice", 5, window));
            assertEquals(5, results.stream().filter(Boolean::booleanValue).count());
            assertTrue(redis.pttl(prefix + ":limit:alice") > 0);

            await().atMost(Duration.ofSeconds(5)).until(() -> redis.get(prefix + ":limit:alice") == null);
            assertTrue(first.allow("alice", 5, window));
        }
    }

    @Test
    void expiredOwnerCannotDeleteNewOwnersLease() {
        var lease = new RedisLease(redis, prefix);
        // Удерживаем первую аренду достаточно долго для проверки отказа второго захвата.
        String oldToken = lease.tryAcquire("report", Duration.ofSeconds(1)).orElseThrow();
        assertTrue(lease.tryAcquire("report", Duration.ofSeconds(5)).isEmpty());
        await().atMost(Duration.ofSeconds(4)).until(() -> redis.get(prefix + ":lease:report") == null);

        String newToken = lease.tryAcquire("report", Duration.ofSeconds(5)).orElseThrow();
        assertFalse(lease.release("report", oldToken));
        assertEquals(newToken, redis.get(prefix + ":lease:report"));
        assertTrue(lease.release("report", newToken));
    }

    @Test
    void concurrentReservationsCannotOversellOrChargeSameRequestTwice() throws Exception {
        try (var peer = client.connect()) {
            var first = new StockReservations(redis, prefix);
            var second = new StockReservations(peer.sync(), prefix);
            assertTrue(first.initialize("sku", 1));
            assertFalse(second.initialize("sku", 2));

            var results = parallelRequests(20,
                    index -> (index % 2 == 0 ? first : second).reserve("sku", "request-" + index));
            assertEquals(1, results.stream().filter(RESERVED::equals).count());
            assertEquals(19, results.stream().filter(SOLD_OUT::equals).count());
            assertEquals("0", redis.hget(prefix + ":stock:sku", "available"));
            int winner = results.indexOf(RESERVED);
            assertEquals(ALREADY_RESERVED, second.reserve("sku", "request-" + winner));
        }
    }

    @Test
    void streamReclaimsPendingDeliveryAfterDatabaseCommitAndAcknowledgesIt() {
        try (var database = new WalletTestDatabase(); var workerA = client.connect(); var workerB = client.connect()) {
            String key = prefix + ":credits";
            String group = "wallet";
            var first = new WalletStream(workerA.sync(), key, group);
            var second = new WalletStream(workerB.sync(), key, group);
            first.createGroup();
            String streamId = first.publish("event-1", 100);
            database.openWallet("alice");
            var ledger = database.ledger();

            var original = first.readNew("worker-a");
            assertEquals(1, original.size());
            var event = original.get(0).getBody();
            assertTrue(ledger.applyOnce(event.get("eventId"), "alice", Long.parseLong(event.get("amount"))));
            // Здесь имитируем падение A после commit БД и перед XACK.
            assertEquals(1, redis.xpending(key, group).getCount());
            assertTrue(second.readNew("worker-b").isEmpty());

            // Нулевой minIdle нужен для детерминированного опыта: A уже объявлен упавшим.
            // В рабочей системе порог должен учитывать нормальное время обработки.
            var claimed = second.reclaim("worker-b", Duration.ZERO, "0-0");
            assertEquals(1, claimed.getMessages().size());
            var retry = claimed.getMessages().get(0);
            assertEquals(streamId, retry.getId());
            assertEquals(event, retry.getBody());
            assertFalse(ledger.applyOnce(event.get("eventId"), "alice", Long.parseLong(event.get("amount"))));
            assertTrue(second.acknowledge(retry.getId()));
            assertEquals(0, redis.xpending(key, group).getCount());
            assertEquals(1, redis.xlen(key));
            assertEquals(100, database.balance("alice"));
        }
    }

    private static <T> List<T> parallelRequests(int count, IntFunction<T> action) throws Exception {
        var pool = Executors.newFixedThreadPool(8);
        var start = new CountDownLatch(1);
        try {
            var futures = new ArrayList<java.util.concurrent.Future<T>>();
            for (int i = 0; i < count; i++) {
                int index = i;
                futures.add(pool.submit(() -> {
                    start.await();
                    return action.apply(index);
                }));
            }
            start.countDown();
            var results = new ArrayList<T>();
            for (var future : futures) {
                results.add(future.get(5, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
