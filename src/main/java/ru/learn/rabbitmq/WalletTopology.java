package ru.learn.rabbitmq;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.Map;

/** Три очереди для обработки пополнений, отложенных повторов и окончательных ошибок. */
public record WalletTopology(String prefix) {
    public static final String CREDIT = "wallet.credit";
    public static final String RETRY = "wallet.retry";
    public static final String DEAD = "wallet.dead";

    public String exchange() {
        return prefix + ".exchange";
    }

    public String workQueue() {
        return prefix + ".work";
    }

    public String retryQueue() {
        return prefix + ".retry";
    }

    public String deadQueue() {
        return prefix + ".dlq";
    }

    public void declare(Channel channel, int retryDelayMillis) throws IOException {
        // Direct exchange сравнивает routing key с binding key. Сам exchange сообщения не хранит.
        channel.exchangeDeclare(exchange(), BuiltinExchangeType.DIRECT, true);
        // Durable очереди переживают рестарт брокера. Для самих сообщений нужен deliveryMode=2.
        channel.queueDeclare(workQueue(), true, false, false, Map.of(
                "x-queue-type", "classic",
                "x-dead-letter-exchange", exchange(),
                "x-dead-letter-routing-key", DEAD));
        channel.queueDeclare(retryQueue(), true, false, false, Map.of(
                "x-queue-type", "classic",
                "x-message-ttl", retryDelayMillis,
                "x-dead-letter-exchange", exchange(),
                "x-dead-letter-routing-key", CREDIT));
        channel.queueDeclare(deadQueue(), true, false, false, Map.of("x-queue-type", "classic"));
        channel.queueBind(workQueue(), exchange(), CREDIT);
        channel.queueBind(retryQueue(), exchange(), RETRY);
        channel.queueBind(deadQueue(), exchange(), DEAD);
    }
}
