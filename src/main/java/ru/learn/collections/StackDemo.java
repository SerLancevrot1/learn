package ru.learn.collections;

import java.util.ArrayDeque;
import java.util.Deque;

/** Показывает стек LIFO. Последний добавленный элемент снимается первым. */
public class StackDemo {

    public static void main(String[] args) {
        Deque<Integer> deque = new ArrayDeque<>();

        // push добавляет в начало deque. Вершиной после четырёх вызовов станет 4.
        deque.push(1);
        deque.push(2);
        deque.push(3);
        deque.push(4);

        System.out.println(deque);

        // pop работает с тем же концом, поэтому числа выйдут в порядке 4, 3, 2, 1.
        // Проверяем isEmpty перед pop, потому что извлечение из пустого стека бросает исключение.
        while (!deque.isEmpty()) {
            System.out.println(deque.pop());
        }
    }
}
