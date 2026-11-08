package ru.learn.redis;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisCommands;

/** Резервирование одной единицы товара при конкуренции покупателей и повторах запроса. */
public record StockReservations(RedisCommands<String, String> redis, String prefix) {
    private static final String RESERVE = """
            if redis.call('HEXISTS', KEYS[1], ARGV[1]) == 1 then
                return 2
            end
            local available = tonumber(redis.call('HGET', KEYS[1], 'available') or '0')
            if available <= 0 then
                return 0
            end
            redis.call('HSET', KEYS[1], 'available', available - 1, ARGV[1], '1')
            return 1
            """;

    public enum Result {
        SOLD_OUT, RESERVED, ALREADY_RESERVED
    }

    /** Инициализация только для нового SKU. HSETNX защищает остаток при повторном вызове. */
    public boolean initialize(String sku, int available) {
        if (available < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        return redis.hsetnx(key(sku), "available", Integer.toString(available));
    }

    public Result reserve(String sku, String requestId) {
        // Остаток и маркер запроса в одном hash. Проверка и запись выполняются без гонки.
        // Повтор проверяется первым: клиент мог потерять ответ уже успешной покупки последней единицы.
        long result = redis.eval(RESERVE, ScriptOutputType.INTEGER,
                new String[]{key(sku)}, "reservation:" + requestId);
        return switch ((int) result) {
            case 0 -> Result.SOLD_OUT;
            case 1 -> Result.RESERVED;
            case 2 -> Result.ALREADY_RESERVED;
            default -> throw new IllegalStateException("Unexpected reservation result: " + result);
        };
    }

    private String key(String sku) {
        return prefix + ":stock:" + sku;
    }
}
