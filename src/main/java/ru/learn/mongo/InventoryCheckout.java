package ru.learn.mongo;

import com.mongodb.ReadConcern;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.inc;

/** Оформление заказа связывает запись заказа и изменение остатков нескольких товаров. */
public record InventoryCheckout(MongoClient client, MongoCollection<Document> stock,
                                MongoCollection<Document> orders) {
    private static final TransactionOptions OPTIONS = TransactionOptions.builder()
            .readConcern(ReadConcern.SNAPSHOT)
            .writeConcern(WriteConcern.MAJORITY)
            .build();

    public void addSku(String sku, int available) {
        if (available < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        stock.insertOne(new Document("_id", sku).append("available", available));
    }

    public boolean reserveOneSku(String sku, int quantity) {
        // Условие и $inc выполняются атомарно для документа. GET + SET допускал бы продажу сверх остатка.
        return stock.updateOne(hasStock(sku, quantity), inc("available", -quantity)).getModifiedCount() == 1;
    }

    /** Возвращает false при повторе уже оформленного заказа с тем же содержимым. */
    public boolean place(String orderId, String customerId, Instant createdAt, List<OrderCatalog.Line> lines) {
        var cart = List.copyOf(lines);
        Document order = OrderCatalog.order(orderId, customerId, createdAt, cart);
        try (ClientSession session = client.startSession()) {
            // Драйвер может повторно вызвать callback при transient transaction error.
            // Внутри допустимы операции этой MongoDB-транзакции. Внешний HTTP-вызов мог бы повториться.
            return session.withTransaction(() -> {
                Document existing = orders.find(session, eq("_id", orderId)).first();
                if (existing != null) {
                    if (!Objects.equals(existing.get("customerId"), customerId)
                            || !Objects.equals(existing.get("lines"), order.get("lines"))) {
                        throw new IllegalArgumentException("Order id already has another cart: " + orderId);
                    }
                    return false;
                }
                for (var line : cart) {
                    var result = stock.updateOne(session, hasStock(line.sku(), line.quantity()),
                            inc("available", -line.quantity()));
                    if (result.getModifiedCount() == 0) {
                        // Исключение отменит и предыдущие успешные $inc той же транзакции.
                        throw new OutOfStock(line.sku());
                    }
                }
                // Передаём session всем операциям. Без неё запись выполнилась бы вне транзакции.
                orders.insertOne(session, order);
                return true;
            }, OPTIONS);
        }
    }

    private static Bson hasStock(String sku, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        return and(eq("_id", sku), gte("available", quantity));
    }

    public static class OutOfStock extends RuntimeException {
        public OutOfStock(String sku) {
            super("Out of stock: " + sku);
        }
    }
}
