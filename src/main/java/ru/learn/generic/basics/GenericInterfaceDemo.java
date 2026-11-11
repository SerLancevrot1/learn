package ru.learn.generic.basics;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Показывает интерфейс каталога с типами ключа и значения.
 * Одна реализация работает и с номерами заказов, и с текстовыми ключами настроек.
 */
public class GenericInterfaceDemo {

    public static void main(String[] args) {
        Catalog<Long, String> orders = new InMemoryCatalog<>();
        orders.put(42L, "Отправлен");
        System.out.println("Статус заказа: " + orders.find(42L).orElse("Не найден"));

        Catalog<String, Integer> settings = new InMemoryCatalog<>();
        settings.put("retries", 3);
        System.out.println("Число попыток: " + settings.find("retries").orElse(0));
    }

    /** Типы K и V задают контракт поиска и записи. */
    private interface Catalog<K, V> {
        void put(K key, V value);

        Optional<V> find(K key);
    }

    /** Передаёт параметры интерфейса в типизированную коллекцию. */
    private static class InMemoryCatalog<K, V> implements Catalog<K, V> {
        private final Map<K, V> entries = new HashMap<>();

        @Override
        public void put(K key, V value) {
            entries.put(key, value);
        }

        @Override
        public Optional<V> find(K key) {
            return Optional.ofNullable(entries.get(key));
        }
    }
}
