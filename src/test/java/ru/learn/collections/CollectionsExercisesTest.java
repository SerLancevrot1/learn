package ru.learn.collections;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionsExercisesTest {
    @Test
    void distinctInOrderKeepsOnlyFirstOccurrences() {
        List<String> input = new ArrayList<>(List.of("b", "a", "b", "c", "a"));

        List<String> result = CollectionsExercises.distinctInOrder(input);

        // Сравнение списков проверяет и состав, и порядок. "b" остаётся перед "a".
        assertEquals(List.of("b", "a", "c"), result);
        // Задача просит построить новый список с уникальными элементами, исходник нужен целиком.
        assertEquals(List.of("b", "a", "b", "c", "a"), input);
    }

    @Test
    void frequenciesCountsValuesAndKeepsKeyOrder() {
        Map<String, Integer> counts = CollectionsExercises.frequencies(List.of("b", "a", "b", "c"));

        // Map.equals сравнивает пары ключ-значение. Проверяем порядок отдельно по списку ключей.
        assertEquals(Map.of("a", 1, "b", 2, "c", 1), counts);
        assertEquals(List.of("b", "a", "c"), new ArrayList<>(counts.keySet()));
        assertTrue(CollectionsExercises.frequencies(List.of()).isEmpty());
    }

    @Test
    void acceptsNestedBracketsAndIgnoresOtherCharacters() {
        // Сначала закрывается [], затем (), затем {}. Буквы и цифры пропускаются.
        assertTrue(CollectionsExercises.hasBalancedBrackets("{x([1])}"));
        assertTrue(CollectionsExercises.hasBalancedBrackets(""));
    }

    @Test
    void rejectsCrossedOrUnmatchedBrackets() {
        // Количество открытий и закрытий совпадает, но ']' должно появиться раньше ')'.
        assertFalse(CollectionsExercises.hasBalancedBrackets("([)]"));
        assertFalse(CollectionsExercises.hasBalancedBrackets("]")); // Закрытие при пустом стеке.
        assertFalse(CollectionsExercises.hasBalancedBrackets("([]")); // Открытие осталось в стеке.
    }

    @Test
    void topKPreservesDuplicatesAndReturnsDescendingOrder() {
        List<Integer> input = new ArrayList<>(List.of(-2, 7, 1, 7, 3, -5));

        // Оба числа 7 входят в ответ. После удаления из мин-кучи порядок разворачивается.
        assertEquals(List.of(7, 7, 3), CollectionsExercises.topKLargest(input, 3));
        assertEquals(List.of(-2, 7, 1, 7, 3, -5), input);
    }

    @Test
    void topKHandlesEmptyInputAndBoundarySizes() {
        // При k больше размера возвращаются все числа. Отрицательный k нарушает контракт.
        assertEquals(List.of(-2, -3), CollectionsExercises.topKLargest(List.of(-3, -2), 5));
        assertEquals(List.of(2), CollectionsExercises.topKLargest(List.of(1, 2, 1), 1));
        assertEquals(List.of(), CollectionsExercises.topKLargest(List.of(1), 0));
        assertEquals(List.of(), CollectionsExercises.topKLargest(List.of(), 3));
        assertThrows(IllegalArgumentException.class, () -> CollectionsExercises.topKLargest(List.of(1), -1));
    }

    @Test
    void cacheEvictsLeastRecentlyUsedEntryAfterRead() {
        LruCache<String, Integer> cache = new LruCache<>(2);
        cache.put("a", 1);
        cache.put("b", 2);

        // Чтение делает a самой свежей записью. Третья вставка должна вытеснить b.
        assertEquals(1, cache.get("a"));
        cache.put("c", 3);

        assertNull(cache.get("b"));
        assertEquals(1, cache.get("a"));
        assertEquals(3, cache.get("c"));
    }

    @Test
    void cacheUpdateChangesValueAndAccessOrder() {
        LruCache<String, Integer> cache = new LruCache<>(2);
        cache.put("a", 1);
        cache.put("b", 2);

        // Обновление a сохраняет две записи и отмечает обращение к a.
        cache.put("a", 10);
        cache.put("c", 3);

        // Первой проверяем b. Чтение a до вставки c скрыло бы ошибку в обработке put.
        assertNull(cache.get("b"));
        assertEquals(10, cache.get("a"));
        assertEquals(3, cache.get("c"));
    }

    @Test
    void cacheValidatesArgumentsAndSupportsSingleEntry() {
        assertThrows(IllegalArgumentException.class, () -> new LruCache<>(0));
        LruCache<String, Integer> cache = new LruCache<>(1);
        cache.put("a", 1);

        // Запрещённые аргументы не меняют содержимое. null от get однозначно означает промах.
        assertThrows(NullPointerException.class, () -> cache.put("a", null));
        assertThrows(NullPointerException.class, () -> cache.put(null, 2));
        assertThrows(NullPointerException.class, () -> cache.get(null));
        assertEquals(1, cache.get("a"));

        cache.put("b", 2);
        assertNull(cache.get("a"));
        assertEquals(2, cache.get("b"));
    }
}
