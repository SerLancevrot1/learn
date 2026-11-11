package ru.learn.generic.bounds;

import java.math.BigDecimal;

/**
 * Показывает одиночное ограничение T extends Number и пересечение ограничений.
 * Ограничения дают доступ к нужным методам, а возвращаемый тип остаётся конкретным.
 */
public class BoundedTypesDemo {

    /** Хранит точное значение и позволяет получить приближение через API Number. */
    private record Reading<T extends Number>(T value) {
        double approximateValue() {
            return value.doubleValue();
        }
    }

    public static void main(String[] args) {
        Reading<BigDecimal> price = new Reading<>(new BigDecimal("19.90"));
        BigDecimal exact = price.value();
        System.out.println("Исходное значение: " + exact);
        System.out.println("Как double: " + price.approximateValue());

        BigDecimal largerPrice = largerNumber(exact, new BigDecimal("20.10"));
        System.out.println("Большая цена: " + largerPrice);
        // largerNumber("19.90", "20.10") не скомпилируется. String не наследует Number.
    }

    private static <T extends Number & Comparable<? super T>> T largerNumber(T left, T right) {
        // Number ограничивает область числами. Comparable даёт compareTo(). Граница-класс идёт первой.
        return left.compareTo(right) >= 0 ? left : right;
    }
}
