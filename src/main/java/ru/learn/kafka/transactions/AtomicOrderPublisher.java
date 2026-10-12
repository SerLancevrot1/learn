package ru.learn.kafka.transactions;

import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Демонстрирует producer-транзакцию Kafka: одна операция приложения
 * публикует заказ и аудит-событие как единое целое.
 */
@Service
public class AtomicOrderPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public AtomicOrderPublisher(KafkaTemplate<Object, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Записывает заказ и аудит с одним ключом в одной Kafka-транзакции.
     *
     * Если callback завершится успешно, Spring зафиксирует транзакцию.
     * Если отправка или callback выбросит исключение, Spring отменит её.
     */
    public void publish(String orderId, String orderPayload) {
        kafkaTemplate.executeInTransaction(operations -> {
            // У отправок один ключ: Kafka помещает их в одну и ту же партицию
            // каждого топика, а consumers могут связать записи по orderId.
            operations.send(TransactionTopics.ORDERS, orderId, orderPayload);

            // Это вторая запись той же транзакции. Между двумя send нет commit.
            operations.send(TransactionTopics.ORDER_AUDIT, orderId, "ORDER_ACCEPTED");

            // Нормальное возвращение из callback означает: обе записи готовы к commit.
            return null;
        });
    }
}
