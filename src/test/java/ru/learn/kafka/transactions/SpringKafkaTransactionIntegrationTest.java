package ru.learn.kafka.transactions;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.learn.kafka.KafkaListenerConfiguration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EmbeddedKafka(partitions = 1, topics = {SpringKafkaTransactionIntegrationTest.INPUT,
        SpringKafkaTransactionIntegrationTest.OUTPUT})
@SpringJUnitConfig
@TestPropertySource(properties = {
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.transaction-id-prefix=spring-eos-test-",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.enable-auto-commit=false",
        "spring.kafka.consumer.isolation-level=read_committed"
})
class SpringKafkaTransactionIntegrationTest {
    static final String INPUT = "spring-eos.input";
    static final String OUTPUT = "spring-eos.output";
    private static final String GROUP = "spring-eos.worker";

    @Configuration
    @EnableKafka
    @ImportAutoConfiguration(KafkaAutoConfiguration.class)
    @Import({KafkaListenerConfiguration.class, FailOnceListener.class})
    static class Config {
    }

    @Autowired
    private KafkaTemplate<Object, Object> template;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private FailOnceListener listener;

    @Test
    void springContainerAbortsSeeksAndCommitsOutputWithOffset() throws Exception {
        template.executeInTransaction(ops -> ops.send(INPUT, "order-1", "created").join());

        var output = new TopicPartition(OUTPUT, 0);
        try (var committed = reader("read_committed"); var all = reader("read_uncommitted")) {
            // Успешный результат появляется только после commit контейнера.
            assertEquals(List.of("attempt-2:created"), read(committed, output, 1));
            assertEquals(List.of("attempt-1:created", "attempt-2:created"), read(all, output, 2));
            assertEquals(2, listener.attempts.get());

            var input = new TopicPartition(INPUT, 0);
            // Тот же commit транзакции записал следующий offset группы.
            assertEquals(1L, committed.committed(Set.of(input)).get(input).offset());
        }
    }

    private KafkaConsumer<String, String> reader(String isolationLevel) {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ConsumerConfig.GROUP_ID_CONFIG, GROUP,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.ISOLATION_LEVEL_CONFIG, isolationLevel,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
    }

    private List<String> read(KafkaConsumer<String, String> consumer, TopicPartition partition, int count) {
        // assign без subscribe не присоединяет наблюдателя к рабочей группе и не вызывает rebalance.
        consumer.assign(List.of(partition));
        consumer.seekToBeginning(List.of(partition));
        var values = new ArrayList<String>();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (values.size() < count && System.nanoTime() < deadline) {
            consumer.poll(Duration.ofMillis(200)).forEach(record -> values.add(record.value()));
        }
        return values;
    }

    static class FailOnceListener {
        private final KafkaTemplate<Object, Object> template;
        private final AtomicInteger attempts = new AtomicInteger();

        FailOnceListener(KafkaTemplate<Object, Object> template) {
            this.template = template;
        }

        @KafkaListener(topics = INPUT, groupId = GROUP,
                containerFactory = "exactlyOnceKafkaListenerContainerFactory")
        void process(ConsumerRecord<String, String> record) {
            int attempt = attempts.incrementAndGet();
            template.send(OUTPUT, record.key(), "attempt-" + attempt + ":" + record.value()).join();

            if (attempt == 1) {
                // Запись уже подтверждена брокером, но транзакция ещё открыта.
                // Исключение заставляет контейнер сделать abort и перечитать вход с того же offset.
                throw new IllegalStateException("Crash after send, before Kafka transaction commit");
            }
        }
    }
}
