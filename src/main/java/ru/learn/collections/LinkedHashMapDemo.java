package ru.learn.collections;

import java.util.LinkedHashMap;
import java.util.Map;

/** Показывает обход карты в порядке первой вставки ключей. */
public class LinkedHashMapDemo {
    public static void main(String[] args) {
        // Хеш-таблица отвечает за поиск по ключу, отдельные связи между записями задают порядок обхода.
        // При хорошем распределении хешей get и put обычно занимают O(1), обход всех записей — O(n).
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "1");
        map.put(6, "6");
        map.put(2, "2");
        map.put(3, "3");
        map.put(5, "5");
        map.put(4, "4");

        // Ключи печатаются в порядке 1, 6, 2, 3, 5, 4. Для сортировки по ключу подошёл бы TreeMap.
        map.forEach((key, value) -> System.out.println(key + " " + value));
    }
}
