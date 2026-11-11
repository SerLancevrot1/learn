package ru.learn.cuncurrent.executor;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Показывает результат Callable, ошибку задачи и ограниченное ожидание Future.
 * После таймаута вызывающий код сам решает, запрашивать ли отмену.
 */
public class FutureDemo {

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> total = pool.submit(() -> 1200 + 300);
            System.out.println("Стоимость заказа: " + total.get());

            Future<Integer> broken = pool.submit(() -> {
                throw new IllegalStateException("Тариф не найден");
            });
            try {
                broken.get();
            } catch (ExecutionException e) {
                System.out.println("Причина ошибки: " + e.getCause().getMessage());
            }

            cancelWaitingTask(pool);
        } finally {
            pool.shutdownNow();
        }
    }

    private static void cancelWaitingTask(ExecutorService pool) throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1);
        CountDownLatch remoteResponse = new CountDownLatch(1);
        Future<String> slow = pool.submit(() -> {
            started.countDown();
            try {
                remoteResponse.await(); // Имитируем сервис, от которого ответ так и не пришёл.
                return "Ответ получен";
            } finally {
                stopped.countDown();
            }
        });

        started.await();
        try {
            slow.get(100, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            System.out.println("Истёк таймаут ожидания. Задача ещё выполняется: " + !slow.isDone());
            slow.cancel(true); // Просим прервать выполняющий поток.
        }
        try {
            slow.get();
        } catch (CancellationException e) {
            System.out.println("Future помечен отменённым");
        }

        // Отдельно ждём выхода из задачи. isCancelled() сообщает только состояние Future.
        if (!stopped.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Задача не отреагировала на отмену");
        }
        System.out.println("Задача завершилась");
    }
}
