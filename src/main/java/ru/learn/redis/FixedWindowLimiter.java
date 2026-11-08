package ru.learn.redis;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisCommands;

import java.time.Duration;

/** Общая квота для нескольких экземпляров приложения. Окно начинается с первого запроса. */
public record FixedWindowLimiter(RedisCommands<String, String> redis, String prefix) {
    private static final String COUNT_REQUEST = """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """;

    public boolean allow(String clientId, int limit, Duration window) {
        long millis = window.toMillis();
        if (limit <= 0 || millis <= 0) {
            throw new IllegalArgumentException("Limit and window must be positive");
        }
        // KEYS — имена ключей, ARGV — данные. Redis выполняет INCR и PEXPIRE без вмешательства других клиентов.
        // Последующие запросы не продлевают TTL, иначе при постоянном трафике окно не закрылось бы.
        long count = redis.eval(COUNT_REQUEST, ScriptOutputType.INTEGER,
                new String[]{prefix + ":limit:" + clientId}, Long.toString(millis));
        return count <= limit;
    }
}
