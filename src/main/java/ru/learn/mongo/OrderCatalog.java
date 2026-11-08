package ru.learn.mongo;

import com.mongodb.ExplainVerbosity;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Indexes.*;
import static com.mongodb.client.model.Sorts.descending;

/** Заказ читается целиком, поэтому позиции и цена на момент покупки хранятся в одном документе. */
public record OrderCatalog(MongoCollection<Document> orders) {
    public static final String CUSTOMER_TIMELINE = "customer_timeline";

    public record Line(String sku, int quantity, long unitCents) {
        public Line {
            if (sku == null || sku.isBlank() || quantity <= 0 || unitCents < 0) {
                throw new IllegalArgumentException("Expected SKU, positive quantity and non-negative price");
            }
        }
    }

    public record Cursor(Instant createdAt, String orderId) {
    }

    public void createIndex() {
        // Сначала точное равенство по клиенту, затем ключи сортировки в том же порядке, что и в запросе.
        // _id завершает сортировку: у нескольких заказов может совпасть timestamp с точностью до миллисекунд.
        orders.createIndex(compoundIndex(ascending("customerId"), descending("createdAt", "_id")),
                new IndexOptions().name(CUSTOMER_TIMELINE));
    }

    public void insert(String id, String customerId, Instant createdAt, List<Line> lines) {
        orders.insertOne(order(id, customerId, createdAt, lines));
    }

    public List<Document> page(String customerId, Cursor after, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        return orders.find(pageFilter(customerId, after))
                .sort(descending("createdAt", "_id"))
                .limit(size)
                .into(new ArrayList<>());
    }

    public Document explainFirstPage(String customerId, int size) {
        // План выбирает сервер. Тест сравнивает фактический выигравший план до и после индекса без hint.
        return orders.find(pageFilter(customerId, null)).sort(descending("createdAt", "_id"))
                .limit(size).explain(ExplainVerbosity.EXECUTION_STATS);
    }

    public List<Document> revenueBySku(String customerId) {
        // $match сужает документы до разворачивания массива. $unwind создаёт строку на позицию заказа.
        // Суммируем количество * историческую цену на сервере вместо выгрузки заказов в JVM.
        return orders.aggregate(List.of(
                new Document("$match", new Document("customerId", customerId)),
                new Document("$unwind", "$lines"),
                new Document("$group", new Document("_id", "$lines.sku")
                        .append("revenueCents", new Document("$sum",
                                new Document("$multiply", List.of("$lines.quantity", "$lines.unitCents"))))),
                new Document("$sort", new Document("_id", 1))
        )).into(new ArrayList<>());
    }

    private Bson pageFilter(String customerId, Cursor after) {
        Bson customer = eq("customerId", customerId);
        if (after == null) {
            return customer;
        }
        // Keyset вместо skip: всё строго после курсора в сортировке createdAt DESC, _id DESC.
        Date lastDate = Date.from(after.createdAt());
        return and(customer, or(lt("createdAt", lastDate),
                and(eq("createdAt", lastDate), lt("_id", after.orderId()))));
    }

    static Document order(String id, String customerId, Instant createdAt, List<Line> lines) {
        if (id == null || id.isBlank() || customerId == null || customerId.isBlank() || lines.isEmpty()) {
            throw new IllegalArgumentException("Order id, customer id and at least one line are required");
        }
        return new Document("_id", id)
                .append("customerId", customerId)
                .append("createdAt", Date.from(createdAt))
                .append("lines", lines.stream().map(line -> new Document("sku", line.sku())
                        .append("quantity", line.quantity()).append("unitCents", line.unitCents())).toList());
    }
}
