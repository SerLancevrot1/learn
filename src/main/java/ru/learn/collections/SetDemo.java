package ru.learn.collections;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

/** Уникальность, порядок вставки, поиск в диапазоне и множество констант enum. */
public class SetDemo {
    public static void main(String[] args) {
        List<Integer> orderIds = List.of(42, 17, 42, 31);
        Set<Integer> seen = new HashSet<>();
        for (Integer id : orderIds) {
            if (!seen.add(id)) {
                System.out.println("Повтор заказа: " + id);
            }
        }
        System.out.println("Порядок первого появления: " + new LinkedHashSet<>(orderIds));

        NavigableSet<Integer> sortedIds = new TreeSet<>(orderIds);
        System.out.println("Первый номер больше 17: " + sortedIds.higher(17));
        System.out.println("Номера от 17 включительно до 42: " + sortedIds.subSet(17, true, 42, false));

        EnumSet<Permission> editor = EnumSet.of(Permission.EDIT, Permission.READ);
        System.out.println("Права редактора в порядке объявления: " + editor);
    }

    private enum Permission { READ, EDIT, PUBLISH }
}
