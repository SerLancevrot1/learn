package ru.learn.generic.basics;

import java.util.List;

/**
 * Показывает параметр типа у метода. Один вызов возвращает String,
 * другой возвращает Integer. Сам класс не параметризован.
 */
public class GenericMethodDemo {

    public static void main(String[] args) {
        String title = firstOrDefault(List.of("Основной заголовок", "Второй заголовок"), "Без названия");
        Integer retries = firstOrDefault(List.of(), 3);

        System.out.println("Заголовок: " + title);
        System.out.println("Число попыток: " + retries);
    }

    private static <T> T firstOrDefault(List<T> values, T fallback) {
        // <T> перед возвращаемым типом объявляет переменную типа для этого метода.
        // Один T связывает тип элементов списка, запасного значения и результата.
        return values.isEmpty() ? fallback : values.get(0);
    }
}
