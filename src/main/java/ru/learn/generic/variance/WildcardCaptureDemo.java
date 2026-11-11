package ru.learn.generic.variance;

import java.util.ArrayList;
import java.util.List;

/**
 * Показывает List<?> и захват wildcard. Тип элементов неизвестен снаружи,
 * но внутри generic-метода ему можно дать имя T и переставить существующие элементы.
 */
public class WildcardCaptureDemo {

    public static void main(String[] args) {
        List<String> columns = new ArrayList<>(List.of("Код", "Статус", "Дата"));
        List<Integer> priorities = new ArrayList<>(List.of(1, 2, 3));

        swapEnds(columns);
        swapEnds(priorities);
        System.out.println("Колонки: " + columns);
        System.out.println("Приоритеты: " + priorities);
    }

    private static void swapEnds(List<?> values) {
        // Тип списка не связан с другими аргументами или результатом, поэтому здесь достаточно ?.
        // Помощник захватывает этот неизвестный тип как T.
        swapCaptured(values);
    }

    private static <T> void swapCaptured(List<T> values) {
        if (values.size() < 2) {
            return;
        }
        int lastIndex = values.size() - 1;
        T first = values.get(0);
        values.set(0, values.get(lastIndex));
        values.set(lastIndex, first);
    }
}
