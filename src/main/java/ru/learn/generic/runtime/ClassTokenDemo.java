package ru.learn.generic.runtime;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Хранит настройки разных типов в одном реестре. Class<T> служит ключом,
 * связывает тип значения с ключом и позволяет проверить тип во время выполнения.
 */
public class ClassTokenDemo {

    public static void main(String[] args) {
        Settings settings = new Settings();
        settings.put(String.class, "production");
        settings.put(Duration.class, Duration.ofSeconds(2));

        Duration timeout = settings.find(Duration.class).orElseThrow();
        System.out.println("Среда: " + settings.find(String.class).orElseThrow());
        System.out.println("Таймаут в секундах: " + timeout.toSeconds());
        System.out.println("Число попыток задано: " + settings.find(Integer.class).isPresent());
        // settings.put(Duration.class, "две секунды") не скомпилируется.
    }

    /** Скрывает разнородную Map за типизированным API. Для каждого класса есть один ключ. */
    private static class Settings {
        private final Map<Class<?>, Object> values = new HashMap<>();

        <T> void put(Class<T> type, T value) {
            values.put(type, type.cast(value));
        }

        <T> Optional<T> find(Class<T> type) {
            // Class.cast() проверяет тип значения, полученного как Object, и возвращает T.
            return Optional.ofNullable(type.cast(values.get(type)));
        }
    }
}
