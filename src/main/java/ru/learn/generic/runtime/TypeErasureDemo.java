package ru.learn.generic.runtime;

import java.util.ArrayList;
import java.util.List;

/**
 * Показывает стирание аргументов типа. Два ArrayList с разными типами
 * элементов имеют один класс во время выполнения.
 */
public class TypeErasureDemo {

    public static void main(String[] args) {
        List<String> titles = new ArrayList<>(List.of("Практикум"));
        List<Integer> scores = new ArrayList<>(List.of(10));

        // При компиляции тип элемента известен и у get() разный тип результата.
        String title = titles.get(0);
        int score = scores.get(0);
        System.out.println(title + ", оценка: " + score);
        System.out.println("Классы совпадают: " + (titles.getClass() == scores.getClass()));

        Object unknown = titles;
        if (unknown instanceof List<?> list) {
            // Проверка распознаёт список. Тип его элементов из Object вывести невозможно.
            System.out.println("Объект является List, размер: " + list.size());
        }
        // unknown instanceof List<String> не скомпилируется.
    }
}
