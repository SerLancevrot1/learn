package ru.learn.collections;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Учебный кеш для одного потока. LRU означает, что при превышении лимита удаляется запись,
 * к которой дольше всего не обращались.
 */
public final class LruCache<K, V> {
    private final int maxSize;
    // true включает порядок доступа. Успешный get и put перемещают запись в конец обхода.
    private final Map<K, V> entries = new LinkedHashMap<>(16, 0.75f, true);

    public LruCache(int maxSize) {
        if (maxSize < 1) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        this.maxSize = maxSize;
    }

    /** Возвращает значение и отмечает обращение к записи. null означает промах. */
    public V get(K key) {
        return entries.get(Objects.requireNonNull(key, "key"));
    }

    /** Добавляет или обновляет запись. Ключ и значение должны быть отличны от null. */
    public void put(K key, V value) {
        entries.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
        if (entries.size() > maxSize) {
            // Начало списка доступа занимает запись, которую дольше всего не читали и не обновляли.
            // После next() удаляем именно её через итератор представления keySet().
            Iterator<K> keys = entries.keySet().iterator();
            keys.next();
            keys.remove();
        }
    }
}
