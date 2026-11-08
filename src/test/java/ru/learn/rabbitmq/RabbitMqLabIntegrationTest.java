package ru.learn.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import ru.learn.delivery.WalletTestDatabase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "labs.external", matches = "true")
class RabbitMqLabIntegrationTest {
    private final WalletTopology topology = new WalletTopology("lab." + UUID.randomUUID());
    private Connection connection;

    @BeforeEach
    void connect() throws Exception {
        var factory = new ConnectionFactory();
        factory.setUri(System.getenv().getOrDefault("LAB_RABBIT_URI", "amqp://learn:learn@localhost:5672"));
        // Тест управляет перезапуском явно, чтобы автоматическое восстановление не скрывало этапы.
        factory.setAutomaticRecoveryEnabled(false);
        connection = factory.newConnection();
        try (var channel = connection.createChannel()) {
            topology.declare(channel, 300);
        }
    }

    @AfterEach
    void cleanup() throws Exception {
        if (connection != null) {
            try (var ignored = connection; var channel = connection.createChannel()) {
                channel.queueDelete(topology.workQueue());
                channel.queueDelete(topology.retryQueue());
                channel.queueDelete(topology.deadQueue());
                channel.exchangeDelete(topology.exchange());
            }
        }
    }

    @Test
    void mandatoryReturnDetectsUnroutableMessageDespiteConfirm() throws Exception {
        try (var send = connection.createChannel(); var receive = connection.createChannel()) {
            var publisher = new ConfirmedPublisher(send);
            var failure = assertThrows(IOException.class, () -> publisher.publish(
                    topology.exchange(), "typo", new AMQP.BasicProperties(), new byte[0]));
            assertTrue(failure.getMessage().contains("NO_ROUTE"));

            // Return известен, канал здоров. Исправленный routing key доставляет сообщение.
            publisher.publishCredit(topology, "event-1", "100");
            var delivery = receive.basicGet(topology.workQueue(), false);
            assertNotNull(delivery);
            assertEquals("event-1", delivery.getProps().getMessageId());
            receive.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
    }

    @Test
    void closingChannelAfterDatabaseCommitRedeliversButInboxPreventsDoubleCredit() throws Exception {
        try (var send = connection.createChannel(); var database = new WalletTestDatabase()) {
            var publisher = new ConfirmedPublisher(send);
            var worker = new WalletWorker(topology, publisher, 2);
            database.openWallet("alice");
            var ledger = database.ledger();
            publisher.publishCredit(topology, "event-1", "100");

            try (var first = connection.createChannel()) {
                var delivery = first.basicGet(topology.workQueue(), false);
                assertNotNull(delivery);
                assertFalse(delivery.getEnvelope().isRedeliver());
                assertThrows(IllegalStateException.class, () -> worker.handle(first, delivery, (id, amount) -> {
                    assertTrue(ledger.applyOnce(id, "alice", amount));
                    throw new IllegalStateException("Crash after DB commit, before basic.ack");
                }));
                // Закрытие канала вернёт unacked сообщение в очередь.
            }

            try (var restarted = connection.createChannel()) {
                var redelivery = await().atMost(Duration.ofSeconds(5)).until(
                        () -> restarted.basicGet(topology.workQueue(), false), Objects::nonNull);
                assertTrue(redelivery.getEnvelope().isRedeliver());
                worker.handle(restarted, redelivery,
                        (id, amount) -> assertFalse(ledger.applyOnce(id, "alice", amount)));
                assertNull(restarted.basicGet(topology.workQueue(), false));
            }
            assertEquals(100, database.balance("alice"));
        }
    }

    @Test
    void transientFailureIsRetriedAndThenApplied() throws Exception {
        try (var send = connection.createChannel(); var receive = connection.createChannel();
             var database = new WalletTestDatabase()) {
            var publisher = new ConfirmedPublisher(send);
            var worker = new WalletWorker(topology, publisher, 2);
            var attempts = new AtomicInteger();
            database.openWallet("alice");
            var ledger = database.ledger();
            publisher.publishCredit(topology, "event-1", "100");
            WalletWorker.CreditHandler handler = (id, amount) -> {
                if (attempts.incrementAndGet() == 1) {
                    throw new WalletWorker.RetryableCreditException("Wallet service temporarily unavailable");
                }
                ledger.applyOnce(id, "alice", amount);
            };
            assertTrue(worker.pollOne(receive, handler));
            assertEquals(0, database.balance("alice"));
            // Не используем sleep: брокер самостоятельно переносит сообщение после TTL.
            await().atMost(Duration.ofSeconds(5)).until(() -> worker.pollOne(receive, handler));
            assertEquals(2, attempts.get());
            assertEquals(100, database.balance("alice"));
            assertNull(receive.basicGet(topology.deadQueue(), false));
        }
    }

    @Test
    void permanentFailureAndExhaustedRetriesReachDeadLetterQueue() throws Exception {
        try (var send = connection.createChannel(); var receive = connection.createChannel()) {
            var publisher = new ConfirmedPublisher(send);
            var worker = new WalletWorker(topology, publisher, 2);

            publisher.publishCredit(topology, "malformed", "not-a-number");
            assertTrue(worker.pollOne(receive, (id, amount) -> fail("Malformed message must not reach handler")));
            assertDeadLetter(receive, "malformed", 0);

            publisher.publishCredit(topology, "exhausted", "100");
            var attempts = new AtomicInteger();
            for (int i = 0; i < 3; i++) {
                await().atMost(Duration.ofSeconds(5)).until(() -> worker.pollOne(receive, (id, amount) -> {
                    attempts.incrementAndGet();
                    throw new WalletWorker.RetryableCreditException("Still failing");
                }));
            }
            assertDeadLetter(receive, "exhausted", 2);
            assertEquals(3, attempts.get());
        }
    }

    @Test
    void failedRetryPublicationDoesNotAcknowledgeOriginalDelivery() throws Exception {
        try (var send = connection.createChannel()) {
            var publisher = new ConfirmedPublisher(send);
            var worker = new WalletWorker(topology, publisher, 2);
            publisher.publishCredit(topology, "event-1", "100");

            try (var first = connection.createChannel()) {
                // Имитируем ошибку конфигурации: отправке в retry больше некуда маршрутизироваться.
                first.queueUnbind(topology.retryQueue(), topology.exchange(), WalletTopology.RETRY);
                var failure = assertThrows(IOException.class, () -> worker.pollOne(first, (id, amount) -> {
                    throw new WalletWorker.RetryableCreditException("Try again");
                }));
                assertTrue(failure.getMessage().contains("NO_ROUTE"));
            }

            try (var restarted = connection.createChannel()) {
                var delivery = await().atMost(Duration.ofSeconds(5)).until(
                        () -> restarted.basicGet(topology.workQueue(), false), Objects::nonNull);
                assertEquals("event-1", delivery.getProps().getMessageId());
                assertTrue(delivery.getEnvelope().isRedeliver());
                worker.handle(restarted, delivery, (id, amount) -> {});
            }
        }
    }

    @Test
    void prefetchOneStopsPushDeliveryUntilAck() throws Exception {
        try (var send = connection.createChannel(); var receive = connection.createChannel()) {
            var publisher = new ConfirmedPublisher(send);
            publisher.publishCredit(topology, "first", "100");
            publisher.publishCredit(topology, "second", "100");

            var deliveries = new LinkedBlockingQueue<Delivery>();
            // basic.consume — push. При prefetch=1 брокер ждёт ack первой доставки перед отправкой второй.
            receive.basicQos(1);
            receive.basicConsume(topology.workQueue(), false, (tag, delivery) -> deliveries.add(delivery), tag -> {});
            Delivery first = deliveries.poll(5, TimeUnit.SECONDS);
            assertNotNull(first);
            assertEquals("first", first.getProperties().getMessageId());
            assertNull(deliveries.poll(300, TimeUnit.MILLISECONDS));
            receive.basicAck(first.getEnvelope().getDeliveryTag(), false);

            Delivery second = deliveries.poll(5, TimeUnit.SECONDS);
            assertNotNull(second);
            assertEquals("second", second.getProperties().getMessageId());
            receive.basicAck(second.getEnvelope().getDeliveryTag(), false);
        }
    }

    private void assertDeadLetter(Channel channel, String eventId, int attempt) throws IOException {
        var dead = await().atMost(Duration.ofSeconds(5)).until(
                () -> channel.basicGet(topology.deadQueue(), false), Objects::nonNull);
        assertEquals(eventId, dead.getProps().getMessageId());
        assertEquals(attempt, dead.getProps().getHeaders().get("lab-attempt"));
        assertNotNull(dead.getProps().getHeaders().get("x-death"));
        assertFalse(new String(dead.getBody(), StandardCharsets.UTF_8).isBlank());
        channel.basicAck(dead.getEnvelope().getDeliveryTag(), false);
    }
}
