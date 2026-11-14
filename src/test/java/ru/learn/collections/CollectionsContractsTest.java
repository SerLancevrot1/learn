package ru.learn.collections;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionsContractsTest {
    @Test
    void sameHashCanBelongToDifferentKeys() {
        Map<CustomerKey, String> customers = new HashMap<>();
        customers.put(new CustomerKey(1), "Анна");
        customers.put(new CustomerKey(2), "Борис");

        // Хеши совпадают, но record сравнивает id в equals, поэтому хранятся обе записи.
        // Ищем новым объектом с тем же id, чтобы проверить логическое равенство ключей.
        assertEquals("Анна", customers.get(new CustomerKey(1)));
        assertEquals("Борис", customers.get(new CustomerKey(2)));
        assertEquals(2, customers.size());
    }

    @Test
    void treeSetUsesComparatorToRecognizeDuplicates() {
        Comparator<String> byLength = Comparator.comparingInt(String::length);
        Set<String> words = new TreeSet<>(byLength);

        // Строки отличаются по equals, но сравнение только по длине даёт 0.
        assertTrue(words.add("cat"));
        assertFalse(words.add("dog"));
        assertEquals(List.of("cat"), new ArrayList<>(words));

        // При одинаковой длине сравниваем сами строки. Теперь оба слова остаются в множестве.
        Set<String> byLengthThenText = new TreeSet<>(byLength.thenComparing(Comparator.naturalOrder()));
        byLengthThenText.addAll(List.of("dog", "cat"));
        assertEquals(List.of("cat", "dog"), new ArrayList<>(byLengthThenText));
    }

    @Test
    void keySetIsBackedByTheMapAndGetCanBeAmbiguous() {
        Map<String, Integer> stock = new HashMap<>();
        stock.put("known", null);
        Set<String> keys = stock.keySet();

        // Значение null могло быть записано явно. Наличие ключа проверяем отдельно.
        assertNull(stock.get("known"));
        assertNull(stock.get("missing"));
        assertTrue(stock.containsKey("known"));
        assertFalse(stock.containsKey("missing"));

        // У keys нет отдельной копии записей. Удаление ключа удаляет всю пару из карты.
        assertTrue(keys.remove("known"));
        assertTrue(stock.isEmpty());
    }

    @Test
    void fixedSizeListSharesStorageWithArray() {
        String[] names = {"Анна", "Борис"};
        List<String> view = Arrays.asList(names);

        // Размер взят из массива, но менять значения по индексу можно с обеих сторон.
        view.set(0, "Вера");
        assertEquals("Вера", names[0]);
        names[1] = "Глеб";
        assertEquals("Глеб", view.get(1));
        assertThrows(UnsupportedOperationException.class, () -> view.add("Дина"));
    }

    @Test
    void unmodifiableViewReflectsSourceChangesAndCopyKeepsItsElements() {
        List<String> source = new ArrayList<>(List.of("a"));
        List<String> view = Collections.unmodifiableList(source);
        List<String> snapshot = List.copyOf(source);

        source.add("b");

        // Обёртка читает из живого списка. Копия запоминает, какие ссылки были в списке при создании.
        assertEquals(List.of("a", "b"), view);
        assertEquals(List.of("a"), snapshot);
        assertThrows(UnsupportedOperationException.class, () -> view.set(0, "c"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.set(0, "c"));
    }

    @Test
    void iteratorRemovesCurrentElementAndContinuesTraversal() {
        List<Integer> numbers = new ArrayList<>(List.of(1, 2, 4, 5));
        Iterator<Integer> iterator = numbers.iterator();

        while (iterator.hasNext()) {
            if (iterator.next() % 2 == 0) {
                // Удаляем элемент, который вернул next(). Итератор сам обновляет позицию обхода.
                // Два чётных числа подряд проверяют, что после удаления сосед не пропускается.
                iterator.remove();
            }
        }

        assertEquals(List.of(1, 5), numbers);
    }

    @Test
    void copyOnWriteIteratorTraversesSnapshotAtCreationTime() {
        List<String> subscribers = new CopyOnWriteArrayList<>(List.of("Анна"));
        Iterator<String> currentDispatch = subscribers.iterator();
        subscribers.add("Борис");

        // Итератор рассылки держит старый массив. Новый подписчик доступен при следующем обходе.
        assertEquals("Анна", currentDispatch.next());
        assertFalse(currentDispatch.hasNext());
        assertEquals(List.of("Анна", "Борис"), subscribers);
    }

    private record CustomerKey(int id) {
        @Override
        public int hashCode() {
            // Постоянный хеш создаёт коллизию намеренно. Для обычных ключей нужен хороший разброс.
            return 1;
        }
    }
}
