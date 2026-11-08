package ru.learn.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Return;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/** Публикует по одному сообщению на выделенном канале, чтобы явно показать confirm и return. */
public class ConfirmedPublisher {
    private final Channel channel;
    private final AtomicReference<Return> returned = new AtomicReference<>();
    private boolean uncertain;

    public ConfirmedPublisher(Channel channel) throws IOException {
        this.channel = channel;
        channel.confirmSelect();
        channel.addReturnListener(returned::set);
    }

    public void publishCredit(WalletTopology topology, String eventId, String amount)
            throws IOException, TimeoutException, InterruptedException {
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .messageId(eventId)
                .deliveryMode(2)
                .contentType("text/plain")
                .headers(Map.of("lab-attempt", 0))
                .build();
        publish(topology.exchange(), WalletTopology.CREDIT, props, amount.getBytes(StandardCharsets.UTF_8));
    }

    public synchronized void publish(String exchange, String routingKey, AMQP.BasicProperties props, byte[] body)
            throws IOException, TimeoutException, InterruptedException {
        if (uncertain) {
            throw new IllegalStateException("Publication failed. Reopen the channel before retrying");
        }
        returned.set(null);
        try {
            // mandatory=true: если подходящей очереди нет, брокер пришлёт basic.return.
            channel.basicPublish(exchange, routingKey, true, props, body);
            // У немаршрутизируемого сообщения тоже может быть confirm ACK.
            // RabbitMQ отправляет return перед confirm, а клиент доставляет callback return в этом порядке.
            channel.waitForConfirmsOrDie(5_000);
        } catch (IOException | TimeoutException | InterruptedException | RuntimeException failure) {
            // После таймаута публикация могла пройти. Повторять с тем же eventId на новом канале.
            uncertain = true;
            throw failure;
        }
        Return rejected = returned.get();
        if (rejected != null) {
            throw new IOException("Unroutable message: " + rejected.getReplyText() + " [" + routingKey + "]");
        }
    }
}
