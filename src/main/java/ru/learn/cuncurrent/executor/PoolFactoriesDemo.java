package ru.learn.cuncurrent.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Отправляет одинаковые задачи в single, fixed и cached пулы.
 * Имена потоков помогают увидеть, где выполняется работа.
 */
public class PoolFactoriesDemo {

    public static void main(String[] args) throws Exception {
        // Single выполняет задачи последовательно. Fixed допускает до двух одновременных задач.
        runReports("Single", Executors.newSingleThreadExecutor());
        runReports("Fixed", Executors.newFixedThreadPool(2));

        // Cached переиспользует свободные потоки и создаёт новые, если свободных нет.
        // У него нет заданного верхнего предела на число рабочих потоков.
        runReports("Cached", Executors.newCachedThreadPool());
    }

    private static void runReports(String name, ExecutorService pool) throws Exception {
        try {
            List<Future<?>> reports = new ArrayList<>();
            for (int id = 1; id <= 4; id++) {
                int reportId = id;
                reports.add(pool.submit(() -> {
                    System.out.printf("%s, отчёт %d, поток %s%n",
                            name, reportId, Thread.currentThread().getName());
                }));
            }

            // Отправляем всю пачку до первого get(), чтобы задачи могли выполняться параллельно.
            for (Future<?> report : reports) {
                report.get();
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
