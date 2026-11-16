package ru.learn.collections;

import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;

/** Очередь без ожидания данных и передача работы ожидающему потребителю. */
public class ConcurrentQueuesDemo {
    public static void main(String[] args) throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            nonBlockingQueues(worker);
            synchronousHandoff(worker);
            transferQueue(worker);
        } finally {
            worker.shutdownNow();
        }
    }

    private static void nonBlockingQueues(ExecutorService worker) throws Exception {
        Queue<String> events = new ConcurrentLinkedQueue<>();
        System.out.println("До поступления событий: " + events.poll());
        worker.submit(() -> {
            events.offer("Заказ 42 оплачен");
            events.offer("Заказ 42 передан на склад");
        }).get(5, TimeUnit.SECONDS);

        // Future нужен лишь для воспроизводимого вывода. poll сам по себе не ждёт новых событий.
        System.out.println("Первое событие: " + events.poll());
        System.out.println("Следующее событие: " + events.poll());

        Deque<String> tasks = new ConcurrentLinkedDeque<>();
        tasks.offerLast("Пересчитать статистику");
        worker.submit(() -> tasks.offerFirst("Восстановить обработку заказов")).get(5, TimeUnit.SECONDS);
        System.out.println("С начала deque: " + tasks.pollFirst());
        System.out.println("С конца deque: " + tasks.pollLast());
    }

    private static void synchronousHandoff(ExecutorService worker) throws Exception {
        SynchronousQueue<String> handoff = new SynchronousQueue<>();
        // Без ожидающего получателя offer возвращает false. Хранить задачу здесь негде.
        System.out.println("Передача без получателя: " + handoff.offer("Сформировать выписку"));
        Future<String> receiver = worker.submit(() -> handoff.poll(5, TimeUnit.SECONDS));
        if (!handoff.offer("Сформировать выписку", 5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Получатель не забрал выписку");
        }
        System.out.println("Получена задача: " + receiver.get(5, TimeUnit.SECONDS));
    }

    private static void transferQueue(ExecutorService worker) throws Exception {
        TransferQueue<String> tasks = new LinkedTransferQueue<>();
        tasks.put("Фоновая сверка"); // Обычное добавление в буфер возможно без получателя.
        System.out.println("Немедленная передача без получателя: " + tasks.tryTransfer("Срочная сверка"));
        System.out.println("Из буфера: " + tasks.poll());

        Future<String> receiver = worker.submit(() -> tasks.poll(5, TimeUnit.SECONDS));
        if (!tasks.tryTransfer("Срочная сверка", 5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Получатель не забрал срочную сверку");
        }
        System.out.println("Передано потребителю: " + receiver.get(5, TimeUnit.SECONDS));
    }
}
