package ru.learn.delivery;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class WalletLedgerTest {
    @Test
    void rollbackReleasesInboxMarkerForRetry() {
        try (var database = new WalletTestDatabase()) {
            var ledger = database.ledger();
            assertThrows(IllegalStateException.class, () -> ledger.applyOnce("event-1", "alice", 100));
            database.openWallet("alice");

            // Если бы маркер был зафиксирован отдельно, повтор был бы потерян.
            assertTrue(ledger.applyOnce("event-1", "alice", 100));
            assertFalse(ledger.applyOnce("event-1", "alice", 100));
            assertEquals(100, database.balance("alice"));
        }
    }

    @Test
    void concurrentConsumersCommitOneCredit() throws Exception {
        try (var database = new WalletTestDatabase()) {
            database.openWallet("alice");
            var ledger = database.ledger();
            var start = new CountDownLatch(1);
            var pool = Executors.newFixedThreadPool(2);
            try {
                var first = pool.submit(() -> {
                    start.await();
                    return ledger.applyOnce("event-1", "alice", 100);
                });
                var second = pool.submit(() -> {
                    start.await();
                    return ledger.applyOnce("event-1", "alice", 100);
                });
                start.countDown();

                assertNotEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
                assertEquals(100, database.balance("alice"));
            } finally {
                pool.shutdownNow();
            }
        }
    }
}
