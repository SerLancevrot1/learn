package ru.learn.cuncurrent.executor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Запускает один отчёт с задержкой и периодический сбор метрик.
 * ScheduledFuture позволяет остановить следующие запуски периодической задачи.
 */
public class ScheduledTasksDemo {

    public static void main(String[] args) throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            ScheduledFuture<?> report = scheduler.schedule(() -> {
                System.out.println("Отложенный отчёт готов");
            }, 50, TimeUnit.MILLISECONDS);

            CountDownLatch samples = new CountDownLatch(3);
            // Fixed delay считает паузу от конца предыдущего запуска. Fixed rate задаёт период старта.
            ScheduledFuture<?> metrics = scheduler.scheduleWithFixedDelay(() -> {
                long remaining = samples.getCount();
                if (remaining > 0) {
                    System.out.println("Метрика " + (4 - remaining) + " собрана");
                    samples.countDown();
                }
            }, 0, 50, TimeUnit.MILLISECONDS);

            report.get();
            if (!samples.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Метрики не собраны вовремя");
            }
            metrics.cancel(false);
            System.out.println("Сбор метрик остановлен");
        } finally {
            scheduler.shutdownNow();
        }
    }
}
