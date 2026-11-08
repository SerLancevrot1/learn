package ru.learn.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

/** Один шаг обработчика. Тест управляет временем доставки и закрытием канала между шагами. */
public record WalletWorker(WalletTopology topology, ConfirmedPublisher publisher, int maxRetries) {
    @FunctionalInterface
    public interface CreditHandler {
        void credit(String eventId, long amount) throws RetryableCreditException;
    }

    /** Только ожидаемый временный бизнес-сбой направляется на отложенную повторную попытку. */
    public static final class RetryableCreditException extends Exception {
        public RetryableCreditException(String message) {
            super(message);
        }
    }

    public boolean pollOne(Channel consumerChannel, CreditHandler handler)
            throws IOException, TimeoutException, InterruptedException {
        // basic.get упрощает воспроизводимый тест. Для постоянного потока используют basic.consume и prefetch.
        // false означает manual ack: до подтверждения брокер держит доставку как unacked.
        GetResponse delivery = consumerChannel.basicGet(topology.workQueue(), false);
        if (delivery == null) {
            return false;
        }
        handle(consumerChannel, delivery, handler);
        return true;
    }

    /**
     * При инфраструктурной ошибке исключение уходит вызывающему. Закройте consumer channel,
     * чтобы брокер вернул неподтверждённую доставку в очередь, и перезапустите с задержкой.
     */
    public void handle(Channel channel, GetResponse delivery, CreditHandler handler)
            throws IOException, TimeoutException, InterruptedException {
        long tag = delivery.getEnvelope().getDeliveryTag();
        String eventId = delivery.getProps().getMessageId();
        Long amount = readAmount(delivery.getBody());
        if (eventId == null || eventId.isBlank() || amount == null || amount <= 0) {
            // requeue=false направляет некорректную запись в DLX. Повтор в рабочей очереди её не исправит.
            channel.basicReject(tag, false);
            return;
        }

        try {
            handler.credit(eventId, amount);
        } catch (RetryableCreditException failure) {
            AMQP.BasicProperties properties = delivery.getProps();
            var headers = properties.getHeaders() == null
                    ? new HashMap<String, Object>() : new HashMap<>(properties.getHeaders());
            int attempt = headers.get("lab-attempt") instanceof Number number ? number.intValue() : 0;
            if (attempt >= maxRetries) {
                channel.basicReject(tag, false);
                return;
            }

            // Повтор — новая публикация с тем же messageId. TTL очереди вернёт её в рабочую через DLX.
            headers.put("lab-attempt", attempt + 1);
            publisher.publish(topology.exchange(), WalletTopology.RETRY,
                    properties.builder().headers(headers).build(), delivery.getBody());
            // Между confirm повтора и ack исходника возможен дубль. Inbox защищает бизнес-эффект.
        }
        // Тег действует только на канале доставки. multiple=false подтверждает одно сообщение.
        channel.basicAck(tag, false);
    }

    private static Long readAmount(byte[] body) {
        try {
            return Long.parseLong(new String(body, StandardCharsets.UTF_8));
        } catch (NumberFormatException malformed) {
            return null;
        }
    }
}
