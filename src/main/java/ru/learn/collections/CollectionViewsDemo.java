package ru.learn.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Изменения исходника видны в представлениях. Копия хранит прежний состав. */
public class CollectionViewsDemo {
    public static void main(String[] args) {
        List<String> cart = new ArrayList<>(List.of("Монитор", "Кабель", "Мышь"));
        List<String> firstItems = cart.subList(0, 2);
        List<String> liveView = Collections.unmodifiableList(cart);
        List<String> receipt = List.copyOf(cart);

        firstItems.set(1, "Переходник"); // Запись через subList меняет исходный список.
        cart.removeIf("Мышь"::equals);
        System.out.println("Корзина сейчас: " + liveView);
        System.out.println("Состав на момент выписки: " + receipt);

        Map<Integer, String> customers = new LinkedHashMap<>();
        customers.put(42, "Покупатель");
        customers.put(17, "Тестовый аккаунт");
        customers.keySet().remove(17);
        System.out.println("Записи после удаления ключа из представления: " + customers);
    }
}
