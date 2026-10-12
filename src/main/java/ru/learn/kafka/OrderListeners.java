package ru.learn.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Два демонстрационных consumer-а читают один входной топик разными consumer groups.
 * Поэтому Kafka доставляет каждое событие обоим listener-ам независимо.
 */
@Component
public class OrderListeners {

    // KafkaTemplate отправляет выходную запись в транзакцию контейнера при EOS listener-е.
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    // Spring внедряет тот же template, которым пользуются producer и транзакционный listener.
    public OrderListeners(KafkaTemplate<Object, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Эта группа имеет собственный offset и не делит его с exactly-once группой.
    @KafkaListener(
            topics = KafkaTopics.ORDERS,
            groupId = "learn-orders-at-least-once",
            containerFactory = "atLeastOnceKafkaListenerContainerFactory")
    public void consumeAtLeastOnce(ConsumerRecord<Object, Object> record, Acknowledgment acknowledgment) {
        // Если обработка бросит исключение до следующей строки, offset не подтвердится
        // и Kafka может доставить запись повторно после перезапуска/повторной попытки.
        processOrder(record);
        // Ручное подтверждение говорит контейнеру, что обработка успешно закончилась.
        acknowledgment.acknowledge();
    }

    // Вторая, независимая группа показывает транзакционную обработку Kafka -> Kafka.
    @KafkaListener(
            topics = KafkaTopics.ORDERS,
            groupId = "learn-orders-exactly-once",
            containerFactory = "exactlyOnceKafkaListenerContainerFactory")
    public void consumeExactlyOnce(ConsumerRecord<Object, Object> record) {
        // Исключение здесь заставляет контейнер отменить транзакцию и не фиксировать offset.
        processOrder(record);
        // Запись попадёт в транзакцию контейнера: выход и входной offset фиксируются атомарно.
        kafkaTemplate.send(KafkaTopics.PROCESSED_ORDERS, record.key(), record.value());
    }

    /** Простая проверка входа; бизнес-логику обработки можно добавить сюда. */
    private void processOrder(ConsumerRecord<Object, Object> record) {
        // Kafka допускает null key/value, но наш учебный формат заказа — нет.
        if (record.key() == null || record.value() == null) {
            // Не подтверждаем некорректное сообщение: контейнер применит настроенную обработку ошибки.
            throw new IllegalArgumentException("Order key and payload must not be null");
        }
    }
}
