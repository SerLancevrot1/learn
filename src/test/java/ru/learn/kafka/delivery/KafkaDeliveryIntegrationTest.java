package ru.learn.kafka.delivery;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.learn.delivery.WalletTestDatabase;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static ru.learn.kafka.delivery.OffsetCommitLab.CommitOrder.*;

@EmbeddedKafka(partitions = 1, topics = {"delivery.loss", "delivery.duplicate", "delivery.inbox"})
@SpringJUnitConfig
class KafkaDeliveryIntegrationTest {
    @Configuration
    static class Config {
    }

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    void commitBeforeProcessingLosesBusinessEffectOnCrash() throws Exception {
        var partition = new TopicPartition("delivery.loss", 0);
        String group = UUID.randomUUID().toString();
        publish(partition, "credit", "next");

        try (var first = consumer(group, partition)) {
            assertThrows(SimulatedCrash.class, () -> processNext(first, BEFORE_PROCESSING, record -> {
                assertEquals("credit", record.key());
                throw new SimulatedCrash();
            }));
            assertEquals(1, first.committed(java.util.Set.of(partition)).get(partition).offset());
        }

        try (var restarted = consumer(group, partition)) {
            // Запись-маркер доказывает, что consumer читает дальше и пропустил offset 0.
            processNext(restarted, AFTER_PROCESSING, record -> assertEquals("next", record.key()));
        }
    }

    @Test
    void commitAfterProcessingRepeatsExternalEffectOnCrash() throws Exception {
        var partition = new TopicPartition("delivery.duplicate", 0);
        String group = UUID.randomUUID().toString();
        publish(partition, "credit");

        try (var database = new WalletTestDatabase()) {
            database.openWallet("alice");
            try (var first = consumer(group, partition)) {
                assertThrows(SimulatedCrash.class, () -> processNext(first, AFTER_PROCESSING, record -> {
                    database.applyWithoutInbox("alice", Long.parseLong(record.value()));
                    throw new SimulatedCrash();
                }));
                assertNull(first.committed(java.util.Set.of(partition)).get(partition));
            }
            try (var restarted = consumer(group, partition)) {
                processNext(restarted, AFTER_PROCESSING, record -> {
                    assertEquals(0, record.offset());
                    database.applyWithoutInbox("alice", Long.parseLong(record.value()));
                });
            }
            assertEquals(200, database.balance("alice"));
        }
    }

    @Test
    void inboxAbsorbsBothRedeliveryAndSecondPublicationOfSameEvent() throws Exception {
        var partition = new TopicPartition("delivery.inbox", 0);
        String group = UUID.randomUUID().toString();
        // Два отдельных send даже с enable.idempotence=true дают два offset.
        publish(partition, "credit", "credit");

        try (var database = new WalletTestDatabase()) {
            database.openWallet("alice");
            var ledger = database.ledger();
            try (var first = consumer(group, partition)) {
                assertThrows(SimulatedCrash.class, () -> processNext(first, AFTER_PROCESSING, record -> {
                    assertTrue(ledger.applyOnce(record.key(), "alice", Long.parseLong(record.value())));
                    throw new SimulatedCrash();
                }));
            }
            try (var restarted = consumer(group, partition)) {
                for (long expectedOffset : List.of(0L, 1L)) {
                    processNext(restarted, AFTER_PROCESSING, record -> {
                        assertEquals(expectedOffset, record.offset());
                        assertFalse(ledger.applyOnce(record.key(), "alice", Long.parseLong(record.value())));
                    });
                }
                assertEquals(2, restarted.committed(java.util.Set.of(partition)).get(partition).offset());
            }
            assertEquals(100, database.balance("alice"));
        }
    }

    private KafkaConsumer<String, String> consumer(String group, TopicPartition partition) {
        var consumer = new KafkaConsumer<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ConsumerConfig.GROUP_ID_CONFIG, group,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
        // Явное назначение исключает rebalance из опыта. Новый клиент всё равно начинает с commit группы.
        consumer.assign(List.of(partition));
        return consumer;
    }

    private void publish(TopicPartition partition, String... eventIds) throws Exception {
        try (var producer = new KafkaProducer<String, String>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            for (String eventId : eventIds) {
                producer.send(new ProducerRecord<>(partition.topic(), partition.partition(), eventId, "100"))
                        .get(10, TimeUnit.SECONDS);
            }
        }
    }

    private void processNext(KafkaConsumer<String, String> consumer, OffsetCommitLab.CommitOrder order,
                             Consumer<ConsumerRecord<String, String>> action) {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            if (OffsetCommitLab.pollAndProcess(consumer, Duration.ofMillis(200), order, action) > 0) {
                return;
            }
        }
        fail("Kafka record did not arrive before timeout");
    }

    private static class SimulatedCrash extends RuntimeException {
    }
}
