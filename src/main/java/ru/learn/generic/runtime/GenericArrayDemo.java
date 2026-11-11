package ru.learn.generic.runtime;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Показывает, как создать массив через фабрику с конкретным типом.
 * Массив сохраняет тип компонента во время выполнения и проверяет каждую запись.
 */
public class GenericArrayDemo {

    public static void main(String[] args) {
        String[] labels = toArray(List.of("Имя", "Статус"), String[]::new);
        System.out.println("Колонки: " + Arrays.toString(labels));
        System.out.println("Тип элемента массива: " + labels.getClass().getComponentType().getSimpleName());

        // Массивы ковариантны. Ссылка имеет тип Number[], но созданный массив хранит Integer.
        Number[] numbers = new Integer[1];
        try {
            numbers[0] = 2.5;
        } catch (ArrayStoreException e) {
            System.out.println("Запись завершилась ошибкой: " + e.getClass().getSimpleName());
        }
    }

    private static <T> T[] toArray(Collection<? extends T> values, IntFunction<T[]> factory) {
        // new T[...] не скомпилируется. Фабрика знает конкретный тип компонента.
        return values.toArray(factory);
    }
}
