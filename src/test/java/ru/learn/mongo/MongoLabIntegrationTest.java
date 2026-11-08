package ru.learn.mongo;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.OperationType;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.mongodb.client.model.Filters.eq;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "labs.external", matches = "true")
class MongoLabIntegrationTest {
    private static final Instant NOW = Instant.parse("2025-01-10T12:00:00Z");
    private MongoClient client;
    private MongoDatabase db;
    private MongoCollection<Document> orders;
    private MongoCollection<Document> stock;
    private OrderCatalog catalog;
    private InventoryCheckout checkout;

    @BeforeEach
    void connect() {
        client = MongoClients.create(System.getenv().getOrDefault("LAB_MONGO_URI",
                "mongodb://localhost:27017/?replicaSet=lab-rs&serverSelectionTimeoutMS=5000"));
        var database = client.getDatabase("lab_" + UUID.randomUUID().toString().replace("-", ""))
                .withWriteConcern(WriteConcern.MAJORITY);
        // Схему готовим до эксперимента, чтобы в транзакции выполнялась только работа с данными.
        database.createCollection("orders");
        db = database;
        db.createCollection("stock");
        orders = db.getCollection("orders");
        stock = db.getCollection("stock");
        catalog = new OrderCatalog(orders);
        checkout = new InventoryCheckout(client, stock, orders);
    }

    @AfterEach
    void cleanup() {
        if (client != null) {
            try (var ignored = client) {
                if (db != null) {
                    db.drop();
                }
            }
        }
    }

    @Test
    void compoundIndexReducesExaminedDocumentsAndCursorKeepsTimestampTies() {
        var line = new OrderCatalog.Line("tea", 1, 125);
        orders.insertMany(IntStream.range(0, 300)
                .mapToObj(i -> OrderCatalog.order("other-" + i, "bob", NOW, List.of(line))).toList());
        for (String id : List.of("a", "b", "c")) {
            catalog.insert(id, "alice", NOW, List.of(line));
        }
        catalog.insert("older", "alice", NOW.minusSeconds(1), List.of(line));

        Document before = catalog.explainFirstPage("alice", 2);
        catalog.createIndex();
        Document after = catalog.explainFirstPage("alice", 2);
        assertTrue(before.get("queryPlanner", Document.class).toJson().contains("COLLSCAN"));
        assertTrue(after.get("queryPlanner", Document.class).get("winningPlan", Document.class)
                .toJson().contains(OrderCatalog.CUSTOMER_TIMELINE));
        assertTrue(after.get("executionStats", Document.class).getInteger("totalDocsExamined")
                < before.get("executionStats", Document.class).getInteger("totalDocsExamined"));

        var first = catalog.page("alice", null, 2);
        Document last = first.get(1);
        var second = catalog.page("alice",
                new OrderCatalog.Cursor(last.getDate("createdAt").toInstant(), last.getString("_id")), 2);
        assertEquals(List.of("c", "b"), ids(first));
        assertEquals(List.of("a", "older"), ids(second));
    }

    @Test
    void aggregationUsesEmbeddedQuantityAndHistoricalPrice() {
        catalog.insert("o-1", "alice", NOW, List.of(
                new OrderCatalog.Line("tea", 2, 100), new OrderCatalog.Line("cake", 1, 200)));
        catalog.insert("o-2", "alice", NOW, List.of(new OrderCatalog.Line("tea", 1, 150)));
        catalog.insert("o-3", "bob", NOW, List.of(new OrderCatalog.Line("tea", 9, 100)));

        var totals = catalog.revenueBySku("alice");
        assertEquals(List.of("cake", "tea"), ids(totals));
        assertEquals(200L, ((Number) totals.get(0).get("revenueCents")).longValue());
        assertEquals(350L, ((Number) totals.get(1).get("revenueCents")).longValue());
    }

    @Test
    void atomicConditionalUpdatePreventsOverselling() throws Exception {
        checkout.addSku("tea", 1);
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var a = pool.submit(() -> {
                start.await();
                return checkout.reserveOneSku("tea", 1);
            });
            var b = pool.submit(() -> {
                start.await();
                return checkout.reserveOneSku("tea", 1);
            });
            start.countDown();
            assertNotEquals(a.get(5, TimeUnit.SECONDS), b.get(5, TimeUnit.SECONDS));
            assertEquals(0, available("tea"));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void secondSkuFailureRollsBackFirstSkuAndRetryAppliesOnlyOnce() {
        checkout.addSku("tea", 3);
        checkout.addSku("cake", 0);
        var lines = List.of(new OrderCatalog.Line("tea", 2, 100), new OrderCatalog.Line("cake", 1, 200));

        assertThrows(InventoryCheckout.OutOfStock.class, () -> checkout.place("o-1", "alice", NOW, lines));
        assertEquals(3, available("tea"));
        assertEquals(0, orders.countDocuments());

        stock.updateOne(eq("_id", "cake"), new Document("$set", new Document("available", 1)));
        assertTrue(checkout.place("o-1", "alice", NOW, lines));
        assertFalse(checkout.place("o-1", "alice", NOW.plusSeconds(30), lines));
        assertThrows(IllegalArgumentException.class, () -> checkout.place("o-1", "bob", NOW, lines));
        assertEquals(1, available("tea"));
        assertEquals(0, available("cake"));
        assertEquals(1, orders.countDocuments());
    }

    @Test
    void concurrentRetriesWithSameOrderIdApplyOneTransaction() throws Exception {
        checkout.addSku("tea", 2);
        var lines = List.of(new OrderCatalog.Line("tea", 1, 100));
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        java.util.concurrent.Callable<Boolean> request = () -> {
            start.await();
            return checkout.place("o-1", "alice", NOW, lines);
        };
        try {
            var a = pool.submit(request);
            var b = pool.submit(request);
            start.countDown();
            // Два вызова с отдельными sessions. После возможного write conflict драйвер перечитывает заказ.
            assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
            assertEquals(1, available("tea"));
            assertEquals(1, orders.countDocuments());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void changeStreamCanResumeAfterLastHandledEvent() {
        org.bson.BsonDocument resumeToken;
        try (var first = orders.watch().maxAwaitTime(200, TimeUnit.MILLISECONDS).cursor()) {
            catalog.insert("first", "alice", NOW, List.of(new OrderCatalog.Line("tea", 1, 100)));
            var event = await().atMost(Duration.ofSeconds(5)).until(first::tryNext, Objects::nonNull);
            assertEquals(OperationType.INSERT, event.getOperationType());
            assertEquals("first", event.getDocumentKey().getString("_id").getValue());
            // Такой checkpoint сохраняют после обработки. Сбои до сохранения могут привести к повтору.
            resumeToken = event.getResumeToken();
        }

        catalog.insert("second", "alice", NOW, List.of(new OrderCatalog.Line("tea", 1, 100)));
        try (var resumed = orders.watch().resumeAfter(resumeToken).maxAwaitTime(200, TimeUnit.MILLISECONDS).cursor()) {
            var event = await().atMost(Duration.ofSeconds(5)).until(resumed::tryNext, Objects::nonNull);
            assertEquals("second", event.getDocumentKey().getString("_id").getValue());
        }
    }

    private int available(String sku) {
        return stock.find(eq("_id", sku)).first().getInteger("available");
    }

    private static List<String> ids(List<Document> documents) {
        return documents.stream().map(doc -> doc.getString("_id")).toList();
    }
}
