package ru.learn.generic.variance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Показывает PECS при переносе значений. Источник производит значения T,
 * а приёмник может принимать T и его подтипы.
 */
public class PecsCopyDemo {

    public static void main(String[] args) {
        List<Integer> counts = List.of(2, 4);
        List<Number> report = new ArrayList<>(List.of(2.5));
        appendAll(counts, report);

        List<Object> export = new ArrayList<>();
        appendAll(report, export);

        System.out.println("Числа для отчёта: " + report);
        System.out.println("Объекты для выгрузки: " + export);
        // appendAll(report, new ArrayList<Integer>()) не скомпилируется из-за возможных Double.
    }

    private static <T> void appendAll(Collection<? extends T> source, Collection<? super T> target) {
        // Producer Extends позволяет читать элементы источника как T.
        // Consumer Super позволяет записывать T. Читать из target можно с гарантией только Object.
        for (T value : source) {
            target.add(value);
        }
    }
}
