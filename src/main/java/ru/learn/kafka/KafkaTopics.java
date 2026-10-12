package ru.learn.kafka;

/**
 * Имена учебных топиков собраны в одном месте, чтобы не дублировать строки
 * и не ошибиться в названии при отправке и чтении сообщений.
 */
public final class KafkaTopics {

    /** Входной топик: сюда отправляются новые заказы. */
    public static final String ORDERS = "learn.orders";

    /** Выходной топик: сюда exactly-once listener отправляет обработанные заказы. */
    public static final String PROCESSED_ORDERS = "learn.orders.processed";

    /** Класс содержит только константы, поэтому создавать его экземпляры не нужно. */
    private KafkaTopics() {
    }
}
