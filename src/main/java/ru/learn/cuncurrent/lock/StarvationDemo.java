package ru.learn.cuncurrent.lock;

import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Моделирует starvation в очереди одного рабочего потока.
 * Если срочные задания постоянно прибывают, обычное может ждать бесконечно.
 */
public class StarvationDemo {

    /** Порядок констант задаёт приоритет в PriorityQueue. */
    private enum Job { URGENT, REGULAR }

    public static void main(String[] args) {
        serve("Приоритетная очередь", new PriorityQueue<>());
        serve("Очередь FIFO", new ArrayDeque<>());
    }

    private static void serve(String name, Queue<Job> pending) {
        pending.add(Job.REGULAR);
        for (int turn = 1; turn <= 4; turn++) {
            // Перед каждым выбором добавляем новое срочное задание.
            pending.add(Job.URGENT);
            System.out.printf("%s, ход %d: %s%n", name, turn, pending.remove());
        }
        System.out.println("Обычное задание всё ещё ждёт: " + pending.contains(Job.REGULAR));
    }
}
