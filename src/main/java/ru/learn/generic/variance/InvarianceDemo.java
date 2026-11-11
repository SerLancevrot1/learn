package ru.learn.generic.variance;

import java.util.ArrayList;
import java.util.List;

/**
 * Показывает инвариантность. Integer можно присвоить переменной Number,
 * а List<Integer> и List<Number> имеют разные правила записи.
 */
public class InvarianceDemo {

    public static void main(String[] args) {
        List<Integer> counts = new ArrayList<>(List.of(2, 4));
        // List<Number> alias = counts; // Иначе в список целых можно было бы записать 2.5.

        List<? extends Number> view = counts;
        Number firstCount = view.get(0);
        System.out.println("Первое значение через wildcard: " + firstCount);
        // view.add(2.5); // Конкретный тип элемента для этой ссылки неизвестен.

        // У новой коллекции тип элементов Number. Она принимает Integer и Double.
        List<Number> copy = new ArrayList<>(counts);
        copy.add(2.5);
        System.out.println("Исходный список: " + counts);
        System.out.println("Копия с дробным значением: " + copy);
    }
}
