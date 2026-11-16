package ru.learn.collections;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** Группировка заказов, подсчёт по статусам и поиск действующего тарифа. */
public class MapDemo {
    public static void main(String[] args) {
        Map<String, List<Integer>> ordersByCity = new HashMap<>();
        Map<Status, Integer> counts = new EnumMap<>(Status.class);
        List<Order> orders = List.of(new Order(42, "Казань", Status.PAID),
                new Order(17, "Самара", Status.NEW), new Order(31, "Казань", Status.PAID));

        for (Order order : orders) {
            ordersByCity.computeIfAbsent(order.city(), city -> new ArrayList<>()).add(order.id());
            counts.merge(order.status(), 1, Integer::sum);
        }
        System.out.println("Заказы из Казани: " + ordersByCity.getOrDefault("Казань", List.of()));
        System.out.println("Количество по статусам: " + counts);

        NavigableMap<LocalDate, Integer> priceByDate = new TreeMap<>();
        priceByDate.put(LocalDate.of(2025, 4, 1), 150);
        priceByDate.put(LocalDate.of(2025, 1, 1), 120);
        Map.Entry<LocalDate, Integer> activePrice = priceByDate.floorEntry(LocalDate.of(2025, 3, 1));
        System.out.println("Действующий тариф на 1 марта: " + activePrice.getValue());
    }

    private enum Status { NEW, PAID }

    private record Order(int id, String city, Status status) {
    }
}
