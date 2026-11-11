package ru.learn.generic.runtime;

import java.util.ArrayList;
import java.util.List;

/**
 * Объединяет произвольное число списков. Varargs передаёт их в массиве,
 * а @SafeVarargs отмечает, что метод обращается с этим массивом безопасно.
 */
public class GenericVarargsDemo {

    public static void main(String[] args) {
        List<Integer> itemCounts = List.of(2, 4);
        List<Double> weights = List.of(2.5, 3.5);
        List<Number> measurements = concatenate(itemCounts, weights);
        System.out.println("Все измерения: " + measurements);
    }

    @SafeVarargs
    private static <T> List<T> concatenate(List<? extends T>... groups) {
        // Читаем массив groups, а элементы копируем в новый список.
        // Содержимое массива не меняется, сам массив остаётся внутри метода.
        List<T> combined = new ArrayList<>();
        for (List<? extends T> group : groups) {
            combined.addAll(group);
        }
        return combined;
    }
}
