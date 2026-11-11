package ru.learn.generic.bounds;

import java.time.LocalDate;
import java.util.Comparator;

/**
 * Выбирает большее значение по естественному порядку или по переданному Comparator.
 * В обоих случаях сравнение может быть определено для супертипа.
 */
public class OrderingDemo {

    public static void main(String[] args) {
        // LocalDate сравнивается с ChronoLocalDate, поэтому здесь нужен Comparable<? super T>.
        LocalDate latest = max(LocalDate.of(2025, 1, 12), LocalDate.of(2025, 1, 20));
        System.out.println("Более поздняя дата: " + latest);

        Comparator<CharSequence> byLength = Comparator.comparingInt(CharSequence::length);
        // Компаратор для CharSequence умеет сравнивать и String. Результат остаётся String.
        String longest = max("dev", "production", byLength);
        System.out.println("Более длинное имя: " + longest);
    }

    private static <T extends Comparable<? super T>> T max(T first, T second) {
        return max(first, second, Comparator.naturalOrder());
    }

    private static <T> T max(T first, T second, Comparator<? super T> comparator) {
        return comparator.compare(first, second) >= 0 ? first : second;
    }
}
