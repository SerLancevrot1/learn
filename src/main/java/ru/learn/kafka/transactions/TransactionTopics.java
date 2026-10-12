package ru.learn.kafka.transactions;

/**
 * Топики второго урока: основной заказ и связанное с ним событие аудита.
 * Они отличаются от топиков первого примера в ru.learn.kafka.
 */
public final class TransactionTopics {

    /** Основная запись о принятом заказе. */
    public static final String ORDERS = "learn.tx.orders";

    /** Аудит-событие, которое должно появиться вместе с заказом или не появиться вовсе. */
    public static final String ORDER_AUDIT = "learn.tx.order-audit";

    private TransactionTopics() {
        // У класса с константами не должно быть экземпляров.
    }
}
