package ru.learn.generic.variance;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Применяет PECS к функции преобразования. Функция принимает элементы списка
 * и отдаёт значения, которые подходят для нового списка.
 */
public class FunctionVarianceDemo {

    public static void main(String[] args) {
        List<String> tags = List.of("java", "generics");
        Function<CharSequence, Integer> length = CharSequence::length;

        // Функция принимает CharSequence (супертип String) и отдаёт Integer (подтип Number).
        List<Number> lengths = map(tags, length);
        System.out.println("Длины тегов: " + lengths);
    }

    private static <T, R> List<R> map(List<T> values, Function<? super T, ? extends R> mapper) {
        List<R> result = new ArrayList<>(values.size());
        for (T value : values) {
            result.add(mapper.apply(value));
        }
        return result;
    }
}
