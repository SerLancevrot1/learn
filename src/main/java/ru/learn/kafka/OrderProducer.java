package ru.learn.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Учебная обёртка над KafkaTemplate: показывает, как приложение публикует заказ
 * в Kafka. send возвращает future отдельной отправки после завершения транзакции.
 */
@Service
public class OrderProducer {

    // Spring создаёт KafkaTemplate из настроек spring.kafka в application.properties.
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    // Конструкторное внедрение делает зависимость явной и упрощает тестирование сервиса.
    public OrderProducer(KafkaTemplate<Object, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Публикует заказ: orderId становится Kafka key, а payload — value.
     * При стандартной партиции по ключу и неизменном числе партиций записи заказа
     * попадут в одну партицию. Порядок Kafka гарантирует в пределах партиции.
     */
    public CompletableFuture<SendResult<Object, Object>> send(String orderId, String payload) {
        // Создаёт отдельную Kafka-транзакцию для этой отправки и фиксирует её,
        // если callback завершается успешно. Вызов блокируется до завершения commit:
        // future подтверждает отдельный send, а ошибка commit может прийти напрямую.
        return kafkaTemplate.executeInTransaction(
                // Operations — KafkaTemplate, привязанный Spring к открытой транзакции.
                operations -> operations.send(KafkaTopics.ORDERS, orderId, payload));
    }
}
