package ru.learn.collections;

import java.util.ArrayDeque;
import java.util.Queue;

/** Показывает очередь FIFO. Первым забирается тот элемент, который добавили раньше всех. */
public class QueueDemo {

    public static void main(String[] args) {
        // Интерфейс Queue показывает, что пример работает только с операциями очереди.
        // ArrayDeque хранит элементы в растущем кольцевом массиве, offer добавляет в хвост.
        Queue<Integer> queue = new ArrayDeque<>();
        queue.offer(1);
        queue.offer(2);
        queue.offer(3);
        queue.offer(4);
        queue.offer(5);
        queue.offer(6);
        queue.offer(7);

        // poll забирает голову. Элементы выйдут в порядке 1–7, после них появится null.
        // ArrayDeque запрещает хранить null, поэтому этот результат однозначно означает пустую очередь.
        Integer next;
        while ((next = queue.poll()) != null) {
            System.out.println(next);
        }
        System.out.println(next);
    }
}
