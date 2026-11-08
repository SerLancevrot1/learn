package ru.learn.redis;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.sync.RedisCommands;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** Временная аренда ресурса на одном Redis. После истечения TTL работу может начать другой владелец. */
public record RedisLease(RedisCommands<String, String> redis, String prefix) {
    private static final String RELEASE_IF_OWNER = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """;

    public Optional<String> tryAcquire(String resource, Duration ttl) {
        long millis = ttl.toMillis();
        if (millis <= 0) {
            throw new IllegalArgumentException("Lease must last at least one millisecond");
        }
        // Токен уникален для каждого захвата. NX запрещает перезапись, PX ограничивает время владения.
        String token = UUID.randomUUID().toString();
        String result = redis.set(key(resource), token, SetArgs.Builder.nx().px(millis));
        return "OK".equals(result) ? Optional.of(token) : Optional.empty();
    }

    public boolean release(String resource, String token) {
        // Между отдельными GET и DEL аренда могла бы истечь и перейти другому владельцу.
        // Скрипт удаляет ключ только при совпадении токена, атомарно на сервере.
        long removed = redis.eval(RELEASE_IF_OWNER, ScriptOutputType.INTEGER,
                new String[]{key(resource)}, token);
        return removed == 1;
    }

    private String key(String resource) {
        return prefix + ":lease:" + resource;
    }
}
