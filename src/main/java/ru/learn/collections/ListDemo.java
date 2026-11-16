package ru.learn.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/** Доступ по индексу в ArrayList и редактирование на позиции итератора в LinkedList. */
public class ListDemo {
    public static void main(String[] args) {
        List<String> stops = new ArrayList<>(List.of("Вокзал", "Музей", "Парк"));
        List<String> route = new LinkedList<>(stops);
        System.out.println("Вторая остановка: " + stops.get(1));
        stops.set(1, "Новый музей");
        stops.sort(Comparator.naturalOrder());
        System.out.println("Справочник остановок: " + stops);

        ListIterator<String> cursor = route.listIterator();
        while (cursor.hasNext()) {
            if (cursor.next().equals("Музей")) {
                // Курсор уже стоит после музея. В LinkedList добавление в найденное место стоит O(1).
                cursor.add("Библиотека");
            }
        }
        System.out.println("Маршрут с добавленной остановкой: " + route);
    }
}
