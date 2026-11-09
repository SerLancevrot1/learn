package ru.learn.delivery;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@EmbeddedKafka(partitions = 1, topics = "wallet.credit.requested")
@SpringJUnitConfig
class WalletOutboxTest {
    @Configuration
    static class Config {
    }

    private static final String TOPIC = "wallet.credit.requested";
    private static final WalletOutbox.Credit CREDIT = new WalletOutbox.Credit("credit-1", "alice", 100);

    @Autowired
    private EmbeddedKafkaBroker broker;

    private final EmbeddedDatabase database =
            new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
                    .setName("outbox-" + UUID.randomUUID() + ";MODE=PostgreSQL")
                    .addScript("classpath:delivery/wallet-outbox.sql").build();
    private final WalletOutbox outbox = new WalletOutbox(database);

    @AfterEach
    void closeDatabase() {
        database.shutdown();
    }

    @Test
    void businessRequestAndOutboxRollBackTogether() {
        var tx = new TransactionTemplate(new JdbcTransactionManager(database));
        tx.executeWithoutResult(status -> {
            outbox.enqueue(CREDIT);
            status.setRollbackOnly();
        });

        var jdbc = new JdbcTemplate(database);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM lab_credit_request", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM lab_credit_outbox", Integer.class));
        assertFalse(outbox.publishNext(credit -> fail("Rolled back request reached the relay")));
    }

    @Test
    void retryOfTheSameRequestDoesNotEnqueueAnotherEvent() {
        outbox.enqueue(CREDIT);
        outbox.enqueue(CREDIT);
        assertThrows(IllegalArgumentException.class,
                () -> outbox.enqueue(new WalletOutbox.Credit("credit-1", "alice", 200)));

        var published = new ArrayList<WalletOutbox.Credit>();
        assertTrue(outbox.publishNext(published::add));
        outbox.enqueue(CREDIT);
        assertFalse(outbox.publishNext(published::add));
        assertEquals(List.of(CREDIT), published);
    }

    @Test
    void competingRelaySkipsTheLockedEvent() throws Exception {
        outbox.enqueue(CREDIT);
        var enteredPublisher = new CountDownLatch(1);
        var releasePublisher = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var first = executor.submit(() -> outbox.publishNext(credit -> {
                assertEquals(CREDIT, credit);
                enteredPublisher.countDown();
                try {
                    assertTrue(releasePublisher.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }));
            try {
                assertTrue(enteredPublisher.await(5, TimeUnit.SECONDS));
                assertFalse(outbox.publishNext(credit -> fail("Another relay already holds this row")));
            } finally {
                releasePublisher.countDown();
            }
            assertTrue(first.get(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void committedRequestSurvivesBeforeSendAndInboxAbsorbsRetryAfterBrokerAck() {
        outbox.enqueue(CREDIT);

        var jdbc = new JdbcTemplate(database);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM lab_credit_request", Integer.class));
        assertFalse(wasPublished());

        // Новый relay продолжает работу по тому же состоянию БД после сбоя отправителя.
        var restartedRelay = new WalletOutbox(database);
        try (var producer = new KafkaProducer<>(Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                    ProducerConfig.ACKS_CONFIG, "all"), new StringSerializer(), new JsonSerializer<WalletOutbox.Credit>());
             var consumer = new KafkaConsumer<>(Map.of(
                     ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                     ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false), new StringDeserializer(),
                     new JsonDeserializer<>(WalletOutbox.Credit.class, false));
             var consumerDatabase = new WalletTestDatabase()) {
            var partition = new TopicPartition(TOPIC, 0);
            consumer.assign(List.of(partition));
            consumer.seekToBeginning(List.of(partition));
            consumerDatabase.openWallet(CREDIT.walletId());
            var ledger = consumerDatabase.ledger();

            assertThrows(SimulatedCrash.class, () -> restartedRelay.publishNext(credit -> {
                sendAndWait(producer, credit);
                throw new SimulatedCrash();
            }));
            assertFalse(wasPublished());
            var first = KafkaTestUtils.getSingleRecord(consumer, TOPIC, Duration.ofSeconds(10));
            assertEquals(CREDIT, first.value());
            assertTrue(ledger.applyOnce(first.value().eventId(), first.key(), first.value().amount()));

            assertTrue(new WalletOutbox(database).publishNext(credit -> sendAndWait(producer, credit)));
            assertTrue(wasPublished());
            var duplicate = KafkaTestUtils.getSingleRecord(consumer, TOPIC, Duration.ofSeconds(10));
            assertTrue(duplicate.offset() > first.offset());
            assertEquals(CREDIT, duplicate.value());
            assertFalse(ledger.applyOnce(duplicate.value().eventId(), duplicate.key(), duplicate.value().amount()));
            assertEquals(100, consumerDatabase.balance(CREDIT.walletId()));
            assertFalse(restartedRelay.publishNext(credit -> fail("Published request reached the relay again")));
        }
    }

    private boolean wasPublished() {
        return Boolean.TRUE.equals(new JdbcTemplate(database).queryForObject("""
                SELECT published FROM lab_credit_outbox WHERE event_id = ?
                """, Boolean.class, CREDIT.eventId()));
    }

    private static void sendAndWait(KafkaProducer<String, WalletOutbox.Credit> producer, WalletOutbox.Credit credit) {
        try {
            producer.send(new ProducerRecord<>(TOPIC, credit.walletId(), credit)).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class SimulatedCrash extends RuntimeException {
    }
}
