package ru.learn.redis;

import io.lettuce.core.SetArgs;
import io.lettuce.core.api.sync.RedisCommands;

import java.time.Duration;
import java.util.function.LongSupplier;

/** Cache-aside для цены в копейках. Источник цены передаётся отдельно от Redis. */
public class ProductPriceCache {
    private final RedisCommands<String, String> redis;
    private final String prefix;
    private final long ttlMillis;

    public ProductPriceCache(RedisCommands<String, String> redis, String prefix, Duration ttl) {
        if (ttl.toMillis() <= 0) {
            throw new IllegalArgumentException("TTL must be at least one millisecond");
        }
        this.redis = redis;
        this.prefix = prefix;
        this.ttlMillis = ttl.toMillis();
    }

    public long getPrice(String sku, LongSupplier loadFromSource) {
        String key = prefix + ":price:" + sku;
        String cached = redis.get(key);
        if (cached != null) {
            return Long.parseLong(cached);
        }

        // При промахе читаем источник. Одновременные промахи могут нагрузить его несколько раз.
        long price = loadFromSource.getAsLong();
        // SET PX выставляет значение и TTL одной командой. Отдельный PEXPIRE оставил бы окно без TTL.
        redis.set(key, Long.toString(price), SetArgs.Builder.px(ttlMillis));
        return price;
    }

    /** Вызывать после успешного commit источника. TTL ограничивает время жизни уже записанного значения. */
    public void invalidate(String sku) {
        redis.del(prefix + ":price:" + sku);
    }
}
