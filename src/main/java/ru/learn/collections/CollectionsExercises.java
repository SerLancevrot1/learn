package ru.learn.collections;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/** Решения задач на выбор коллекции. Условия и подсказки лежат в README этого пакета. */
public final class CollectionsExercises {
    private CollectionsExercises() {
    }

    /** Возвращает новый список, сохраняя первое появление каждого элемента. */
    public static <T> List<T> distinctInOrder(List<T> values) {
        // LinkedHashSet отбрасывает равные элементы, сохраняя порядок их первой вставки.
        // ArrayList собирает элементы в том же порядке и даёт результат с доступом по индексу.
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    /** Возвращает частоту каждого элемента. Ключи идут в порядке первого появления. */
    public static <T> Map<T, Integer> frequencies(List<T> values) {
        Map<T, Integer> counts = new LinkedHashMap<>();
        for (T value : values) {
            // Для нового ключа merge записывает 1. Для уже посчитанного складывает старый счётчик с 1.
            counts.merge(value, 1, Integer::sum);
        }
        return counts;
    }

    /** Проверяет вложенность (), [] и {} в тексте. Остальные символы пропускает. */
    public static boolean hasBalancedBrackets(String text) {
        Deque<Character> expectedClosings = new ArrayDeque<>();
        for (int index = 0; index < text.length(); index++) {
            char symbol = text.charAt(index);
            switch (symbol) {
                // На вершине стека лежит скобка, которая должна закрыть последнее открытие.
                case '(' -> expectedClosings.push(')');
                case '[' -> expectedClosings.push(']');
                case '{' -> expectedClosings.push('}');
                case ')', ']', '}' -> {
                    // Пустой стек означает лишнее закрытие. Несовпадение означает неверную вложенность.
                    if (expectedClosings.isEmpty() || expectedClosings.pop() != symbol) {
                        return false;
                    }
                }
                default -> {
                    // По условию задачи символы текста между скобками не влияют на результат.
                }
            }
        }
        // Если в стеке ещё есть ожидаемые скобки, часть открытий осталась без пары.
        return expectedClosings.isEmpty();
    }

    /** Возвращает до k наибольших чисел по убыванию. Повторы допустимы, null запрещён. */
    public static List<Integer> topKLargest(List<Integer> values, int k) {
        if (k < 0) {
            throw new IllegalArgumentException("k must be non-negative");
        }
        if (k == 0) {
            return new ArrayList<>();
        }

        // В голове мин-кучи лежит наименьшее из чисел, которые пока входят в top k.
        // Когда куча полна, число должно быть больше головы, чтобы занять её место.
        PriorityQueue<Integer> largest = new PriorityQueue<>();
        for (int value : values) {
            if (largest.size() < k) {
                largest.offer(value);
            } else if (value > largest.peek()) {
                largest.poll();
                largest.offer(value);
            }
        }

        // Итератор кучи не обещает сортировку. Повторный poll выдаёт числа от меньшего к большему.
        List<Integer> result = new ArrayList<>(largest.size());
        while (!largest.isEmpty()) {
            result.add(largest.poll());
        }
        Collections.reverse(result);
        return result;
    }
}
