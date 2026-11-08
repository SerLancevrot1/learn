package ru.learn.redis;

import io.lettuce.core.Consumer;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XAutoClaimArgs;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.models.stream.ClaimedMessages;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Учебный транспорт для пополнений кошелька на Redis Streams. */
public record WalletStream(RedisCommands<String, String> redis, String key, String group) {
    public void createGroup() {
        // MKSTREAM создаёт пустой stream при необходимости, 0-0 включает уже существующие записи.
        redis.xgroupCreate(XReadArgs.StreamOffset.from(key, "0-0"), group, XGroupCreateArgs.Builder.mkstream());
    }

    public String publish(String eventId, long amount) {
        // Redis генерирует ID записи. eventId задаёт приложение и сохраняет его при повторной публикации.
        return redis.xadd(key, Map.of("eventId", eventId, "amount", Long.toString(amount)));
    }

    public List<StreamMessage<String, String>> readNew(String worker) {
        // Символ > означает ещё не доставленные этой группе записи. Redis добавит их в её pending list.
        return redis.xreadgroup(Consumer.from(group, worker), XReadArgs.Builder.count(10),
                XReadArgs.StreamOffset.lastConsumed(key));
    }

    public ClaimedMessages<String, String> reclaim(String worker, Duration minIdle, String cursor) {
        // XAUTOCLAIM переносит старые pending записи другому consumer.
        // Начинаем с 0-0, продолжаем с возвращённого курсора, пока Redis снова не вернёт 0-0.
        return redis.xautoclaim(key,
                XAutoClaimArgs.Builder.xautoclaim(Consumer.from(group, worker), minIdle, cursor).count(10));
    }

    public boolean acknowledge(String streamId) {
        // После commit бизнес-операции удаляем запись из pending list. Сам журнал XACK не изменяет.
        return redis.xack(key, group, streamId) == 1;
    }
}
