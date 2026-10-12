package ru.learn.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
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

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

// Поднимает встроенный Kafka broker и заранее создаёт тестовые топики.
@EmbeddedKafka(partitions = 1, topics = {
        KafkaExactlyOnceIntegrationTest.INPUT_TOPIC,
        KafkaExactlyOnceIntegrationTest.OUTPUT_TOPIC
})
// Поднимает минимальный Spring context, необходимый для внедрения EmbeddedKafkaBroker.
@SpringJUnitConfig(classes = KafkaExactlyOnceIntegrationTest.EmbeddedKafkaTestConfiguration.class)
class KafkaExactlyOnceIntegrationTest {

    // Топик, куда тест посылает исходное событие.
    static final String INPUT_TOPIC = "exactly-once.input";

    // Топик для результата обработки.
    static final String OUTPUT_TOPIC = "exactly-once.output";

    // Spring Kafka test предоставляет адрес случайного локального брокера.
    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    void abortedProcessingIsInvisibleAndInputIsReprocessedOnce() throws Exception {
        // Уникальная группа гарантирует чистые offsets даже при повторном запуске теста.
        String groupId = "exactly-once-" + UUID.randomUUID();

        // Сообщения этого тестового топика лежат в партиции 0.
        TopicPartition inputPartition = new TopicPartition(INPUT_TOPIC, 0);

        // Общие параметры producer-а: адрес брокера, сериализаторы и надёжная отправка.
        Properties producerProperties = producerProperties();

        // Транзакционный producer должен иметь уникальный transactional.id в кластере.
        producerProperties.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "tx-" + UUID.randomUUID());

        // Все клиенты закрываются в конце теста, даже если assertion или отправка завершатся ошибкой.
        try (Producer<String, String> source = new KafkaProducer<>(producerProperties());
             Producer<String, String> transactionalProducer = new KafkaProducer<>(producerProperties);
             Consumer<String, String> input = new KafkaConsumer<>(consumerProperties(groupId, "read_committed"));
             Consumer<String, String> output = new KafkaConsumer<>(
                     consumerProperties("output-" + UUID.randomUUID(), "read_committed"));
             Consumer<String, String> uncommittedOutput = new KafkaConsumer<>(
                     consumerProperties("uncommitted-output-" + UUID.randomUUID(), "read_uncommitted"))) {
            // Отправитель входного события обычный, не транзакционный.
            source.send(new ProducerRecord<>(INPUT_TOPIC, "order-7", "created")).get();

            // Инициализирует transactional.id у producer-а до beginTransaction().
            transactionalProducer.initTransactions();

            // Подписка назначает consumer-у партицию; poll внутри receiveOne ждёт данные и assignment.
            input.subscribe(Collections.singleton(INPUT_TOPIC));
            output.subscribe(Collections.singleton(OUTPUT_TOPIC));
            uncommittedOutput.subscribe(Collections.singleton(OUTPUT_TOPIC));

            // Читаем исходное событие, которое будем обрабатывать.
            ConsumerRecord<String, String> firstDelivery = receiveOne(input);

            // Начинаем первую попытку обработки и записываем результат в транзакции.
            transactionalProducer.beginTransaction();
            transactionalProducer.send(new ProducerRecord<>(OUTPUT_TOPIC,
                    firstDelivery.key(), "processed:" + firstDelivery.value())).get();

            // Имитируем сбой до успешного завершения обработки: результат будет помечен aborted.
            transactionalProducer.abortTransaction();

            // read_committed обязан скрыть результат из отменённой транзакции.
            assertFalse(receiveAvailable(output).iterator().hasNext(),
                    "read_committed must hide output from an aborted transaction");

            // read_uncommitted может читать и незавершённые/отменённые транзакционные записи.
            assertEquals("processed:created", receiveOne(uncommittedOutput).value(),
                    "read_uncommitted can observe output from an aborted transaction");

            // Возвращаем входной consumer на offset первого сообщения, имитируя повторную доставку.
            input.seek(inputPartition, firstDelivery.offset());

            // Повторно читаем ту же запись и проверяем, что её offset не изменился.
            ConsumerRecord<String, String> retry = receiveOne(input);
            assertEquals(firstDelivery.offset(), retry.offset());

            // Во второй попытке снова начинаем транзакцию и записываем результат.
            transactionalProducer.beginTransaction();
            transactionalProducer.send(new ProducerRecord<>(OUTPUT_TOPIC,
                    retry.key(), "processed:" + retry.value())).get();

            // Kafka хранит следующий offset для чтения, поэтому к offset записи прибавляем 1.
            Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
            offsets.put(inputPartition, new OffsetAndMetadata(retry.offset() + 1));

            // Записываем offset входной группы в ту же транзакцию, что и результат обработки.
            transactionalProducer.sendOffsetsToTransaction(offsets, input.groupMetadata());

            // Атомарно фиксируем и выходное событие, и входной offset.
            transactionalProducer.commitTransaction();

            // У read_committed consumer-а теперь становится виден успешно зафиксированный результат.
            ConsumerRecord<String, String> committedOutput = receiveOne(output);
            assertEquals("processed:created", committedOutput.value());

            // Проверяем, что второй выходной записи нет: aborted запись невидима, committed только одна.
            assertFalse(receiveAvailable(output).iterator().hasNext(),
                    "only the committed transaction must be visible");

            // Проверяем, что offset группы зафиксирован вместе с выходным событием.
            assertEquals(1L, input.committed(inputPartition).offset());
        }
    }

    /** Читает следующую доступную запись или завершает тест ошибкой по таймауту. */
    private ConsumerRecord<String, String> receiveOne(Consumer<String, String> consumer) {
        // Ограничиваем ожидание, чтобы ошибка теста не зависала навсегда.
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            // poll не только получает данные, но и участвует в назначении партиций consumer-у.
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
            if (!records.isEmpty()) {
                // Для данного теста в ответе достаточно первой записи.
                return records.iterator().next();
            }
        }
        // Нет записи в срок — это ошибка сценария, а не успешный пустой результат.
        throw new AssertionError("Timed out waiting for a Kafka record");
    }

    /** Ждёт ограниченное время, чтобы убедиться, что видимых записей нет. */
    private ConsumerRecords<String, String> receiveAvailable(Consumer<String, String> consumer) {
        // Даём брокеру время показать возможную запись; пустой poll сам по себе ещё не означает отсутствие данных.
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        ConsumerRecords<String, String> records;
        do {
            // Ищем запись до дедлайна, опрашивая брокер небольшими интервалами.
            records = consumer.poll(Duration.ofMillis(200));
        } while (records.isEmpty() && System.nanoTime() < deadline);
        return records;
    }

    /** Формирует клиентские настройки producer-а для именно этого Embedded Kafka. */
    private Properties producerProperties() {
        Properties properties = new Properties();
        // В тесте нельзя использовать внешний localhost:9092 — адрес выдаёт Embedded Kafka.
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        // Kafka работает с байтами, поэтому задаём преобразование String -> bytes для key/value.
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // Требуем подтверждение всех текущих синхронных реплик.
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        // Producer sequence numbers защищают от дублей при повторах отправки.
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return properties;
    }

    /** Настройки consumer-а; isolation.level различает видимость транзакционных записей. */
    private Properties consumerProperties(String groupId, String isolationLevel) {
        Properties properties = new Properties();
        // Соединяемся с брокером, поднятым тестом.
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        // Consumer group хранит независимый offset чтения.
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        // Преобразование байтов Kafka в Java String.
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        // Если у новой группы ещё нет offset, начинаем с самого раннего доступного события.
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Тест вручную управляет offset-ами, не разрешая фоновый commit.
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // read_committed скрывает aborted-транзакции; read_uncommitted позволяет увидеть их.
        properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, isolationLevel);
        return properties;
    }

    // Пустая конфигурация достаточна: Embedded Kafka и тестовые настройки задаёт аннотация выше.
    @Configuration
    static class EmbeddedKafkaTestConfiguration {
    }
}
