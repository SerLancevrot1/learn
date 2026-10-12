package ru.learn.kafka.transactions;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Интеграционный тест: запускает настоящий Kafka broker в процессе теста
 * и проверяет видимость зафиксированных и отменённых записей.
 */
@EmbeddedKafka(partitions = 1, topics = {
        TransactionTopics.ORDERS,
        TransactionTopics.ORDER_AUDIT,
        "learn.tx.rollback"
})
@SpringJUnitConfig(classes = AtomicOrderPublisherIntegrationTest.TestConfiguration.class)
class AtomicOrderPublisherIntegrationTest {

    private static final String ROLLBACK_TOPIC = "learn.tx.rollback";

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private AtomicOrderPublisher publisher;

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Test
    void orderAndAuditBecomeVisibleTogetherAfterCommit() {
        // Подписываемся до публикации, чтобы не пропустить тестовые записи.
        try (Consumer<String, String> consumer = newConsumer("committed-" + UUID.randomUUID(), "read_committed")) {
            consumer.subscribe(Arrays.asList(TransactionTopics.ORDERS, TransactionTopics.ORDER_AUDIT));
            waitForAssignment(consumer);

            // Метод выполняет два send внутри одного executeInTransaction.
            publisher.publish("order-42", "created");

            // После commit обе записи должны появиться у read_committed consumer-а.
            List<ConsumerRecord<String, String>> records = receiveAtLeast(consumer, 2);
            assertEquals(2, records.size());
            assertEquals(1, records.stream()
                    .filter(record -> TransactionTopics.ORDERS.equals(record.topic()))
                    .filter(record -> "created".equals(record.value()))
                    .count());
            assertEquals(1, records.stream()
                    .filter(record -> TransactionTopics.ORDER_AUDIT.equals(record.topic()))
                    .filter(record -> "ORDER_ACCEPTED".equals(record.value()))
                    .count());
            assertEquals("order-42", records.get(0).key());
            assertEquals("order-42", records.get(1).key());
        }
    }

    @Test
    void exceptionInsideTransactionAbortsWrites() {
        // Используем два consumer-а к одному топику, но с разной isolation.level.
        try (Consumer<String, String> committed = newConsumer(
                     "committed-" + UUID.randomUUID(), "read_committed");
             Consumer<String, String> uncommitted = newConsumer(
                     "uncommitted-" + UUID.randomUUID(), "read_uncommitted")) {
            committed.subscribe(Collections.singleton(ROLLBACK_TOPIC));
            uncommitted.subscribe(Collections.singleton(ROLLBACK_TOPIC));
            waitForAssignment(committed);
            waitForAssignment(uncommitted);

            // Runtime exception покидает callback. Spring отменяет транзакцию и
            // передаёт исключение вызывающему коду, поэтому тест ловит его явно.
            assertThrows(IllegalStateException.class, () -> kafkaTemplate.executeInTransaction(operations -> {
                // Дожидаемся подтверждения отправки, чтобы проверить именно abort,
                // а не ошибку, случившуюся до попадания записи к брокеру.
                operations.send(ROLLBACK_TOPIC, "order-99", "must-be-aborted").join();

                // Искусственно имитируем сбой приложения до commit транзакции.
                throw new IllegalStateException("Simulated failure before commit");
            }));

            // Ожидаем, что consumer без фильтра транзакций увидит отправленную запись.
            List<ConsumerRecord<String, String>> aborted = receiveAtLeast(uncommitted, 1);
            assertEquals(1, aborted.size());
            assertEquals("must-be-aborted", aborted.get(0).value());

            // Тот же offset невидим при read_committed: Kafka пометила запись aborted.
            assertFalse(receiveAvailable(committed).iterator().hasNext());
        }
    }

    /** Создаёт consumer с тестовым broker-ом и указанным режимом видимости транзакций. */
    private Consumer<String, String> newConsumer(String groupId, String isolationLevel) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, isolationLevel);
        return new KafkaConsumer<>(properties);
    }

    /** Опросами ждёт, пока Kafka назначит consumer-у его партицию. */
    private void waitForAssignment(Consumer<String, String> consumer) {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (consumer.assignment().isEmpty() && System.nanoTime() < deadline) {
            consumer.poll(Duration.ofMillis(100));
        }
        if (consumer.assignment().isEmpty()) {
            throw new AssertionError("Kafka did not assign a partition before timeout");
        }
    }

    /** Читает записи до получения ожидаемого количества или падает по таймауту. */
    private List<ConsumerRecord<String, String>> receiveAtLeast(
            Consumer<String, String> consumer, int expectedCount) {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        List<ConsumerRecord<String, String>> received = new ArrayList<>();
        while (received.size() < expectedCount && System.nanoTime() < deadline) {
            ConsumerRecords<String, String> polled = consumer.poll(Duration.ofMillis(200));
            polled.forEach(received::add);
        }
        if (received.size() < expectedCount) {
            throw new AssertionError("Expected " + expectedCount + " Kafka records but received " + received.size());
        }
        return received;
    }

    /** Даёт consumer-у время получить доступные записи, чтобы проверить, что он ничего не видит. */
    private ConsumerRecords<String, String> receiveAvailable(Consumer<String, String> consumer) {
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        ConsumerRecords<String, String> records;
        do {
            records = consumer.poll(Duration.ofMillis(200));
        } while (records.isEmpty() && System.nanoTime() < deadline);
        return records;
    }

    @Configuration
    @Import(AtomicOrderPublisher.class)
    static class TestConfiguration {

        @Bean
        ProducerFactory<Object, Object> producerFactory(EmbeddedKafkaBroker broker) {
            // Эти настройки такие же по назначению, как spring.kafka.producer.*:
            // адрес брокера и способ преобразования ключа/значения в байты.
            Map<String, Object> properties = new HashMap<>();
            properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
            properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            properties.put(ProducerConfig.ACKS_CONFIG, "all");
            properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

            // Transaction id prefix включает транзакционный режим и должен быть
            // уникальным для producer-ов, работающих одновременно в broker-е.
            DefaultKafkaProducerFactory<Object, Object> factory =
                    new DefaultKafkaProducerFactory<>(properties);
            factory.setTransactionIdPrefix("atomic-order-" + UUID.randomUUID() + "-");
            return factory;
        }

        @Bean
        KafkaTemplate<Object, Object> kafkaTemplate(ProducerFactory<Object, Object> producerFactory) {
            // Именно этот template использует учебный AtomicOrderPublisher.
            return new KafkaTemplate<>(producerFactory);
        }
    }
}
