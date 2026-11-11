package ru.learn.cuncurrent.coordination;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Четыре задачи делят два подключения к условной базе.
 * Semaphore ограничивает число одновременных пользователей ресурса.
 */
public class SemaphoreDemo {

    public static void main(String[] args) throws Exception {
        Semaphore connections = new Semaphore(2);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        CountDownLatch firstPair = new CountDownLatch(2);

        ExecutorService workers = Executors.newFixedThreadPool(4);
        try {
            List<Future<Void>> queries = new ArrayList<>();
            for (int id = 1; id <= 4; id++) {
                int queryId = id;
                queries.add(workers.submit(() -> {
                    connections.acquire(); // Ждём разрешение до входа в try.
                    try {
                        int concurrentQueries = active.incrementAndGet();
                        peak.accumulateAndGet(concurrentQueries, Math::max);
                        // Только для примера. Первые два запроса пересекутся без подбора sleep().
                        firstPair.countDown();
                        if (!firstPair.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Второй запрос не получил разрешение");
                        }
                        System.out.println("Запрос выполнен: " + queryId);
                    } finally {
                        active.decrementAndGet();
                        connections.release();
                    }
                    return null;
                }));
            }
            for (Future<Void> query : queries) {
                query.get();
            }
            System.out.println("Пик одновременных запросов: " + peak.get());
        } finally {
            workers.shutdownNow();
        }
    }
}
