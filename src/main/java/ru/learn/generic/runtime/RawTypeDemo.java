package ru.learn.generic.runtime;

import java.util.ArrayList;
import java.util.List;

/**
 * Показывает, как raw type обходит проверку типа при записи.
 * Ошибка обнаруживается позднее, когда код пытается прочитать значение как String.
 */
public class RawTypeDemo {

    public static void main(String[] args) {
        List<String> titles = new ArrayList<>(List.of("Отчёт"));
        addThroughRawList(titles);

        System.out.println("Содержимое после записи: " + titles);
        try {
            // Компилятор добавляет приведение к String. Элемент с индексом 1 на деле Integer.
            String title = titles.get(1);
            System.out.println(title);
        } catch (ClassCastException e) {
            System.out.println("Чтение завершилось ошибкой: " + e.getClass().getSimpleName());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"}) // Небезопасная запись нужна только для демонстрации.
    private static void addThroughRawList(List<String> titles) {
        List raw = titles; // У raw List нет аргумента типа. List<?> сохранил бы проверку записи.
        raw.add(42);
    }
}
