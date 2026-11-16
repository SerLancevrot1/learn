package ru.learn.collections;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Показывает обход карты в порядке первой вставки ключей. */
public class LinkedHashMapDemo {
    public static void main(String[] args) {
        // Хеш-таблица отвечает за поиск по ключу, отдельные связи между записями задают порядок обхода.
        // При хорошем распределении хешей get и put обычно занимают O(1), обход всех записей — O(n).
        Map<Integer, String> orders = new LinkedHashMap<>();
        orders.put(42, "Создан");
        orders.put(17, "Отправлен");
        orders.put(31, "Доставлен");
        orders.put(42, "Оплачен"); // Обновление существующего ключа оставляет его на прежнем месте.

        System.out.println("В порядке регистрации: " + orders);
        System.out.println("По номеру заказа: " + new TreeMap<>(orders));
    }
}
