package ru.learn.cuncurrent.executor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Показывает перегрузку пула с ограниченной очередью. Когда очередь заполнена,
 * пул может добавить работника до максимума, а затем отклоняет новые задачи.
 */
public class BoundedPoolDemo {

    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1, 2, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1),
                new ThreadPoolExecutor.AbortPolicy());

        // Удерживаем задачи до демонстрации отказа, чтобы результат не зависел от их скорости.
        CountDownLatch finish = new CountDownLatch(1);
        Runnable processRequest = () -> {
            try {
                finish.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        try {
            pool.execute(processRequest); // Первый работник.
            pool.execute(processRequest); // Единственное место в очереди.
            pool.execute(processRequest); // Второй работник, потому что очередь заполнена.
            System.out.printf("Работников: %d, в очереди: %d%n", pool.getPoolSize(), pool.getQueue().size());
            try {
                pool.execute(processRequest);
            } catch (RejectedExecutionException e) {
                System.out.println("Четвёртый запрос отклонён");
            }

            pool.shutdown(); // Закрываем приём и даём уже принятым задачам закончить.
            finish.countDown();
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Пул не завершился вовремя");
            }
            System.out.println("Выполнено запросов: " + pool.getCompletedTaskCount());
        } finally {
            if (!pool.isTerminated()) {
                pool.shutdownNow();
            }
        }
    }
}
