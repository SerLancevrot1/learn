package ru.learn.cuncurrent.lock;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Показывает livelock. Два работника одновременно захватывают по ресурсу
 * и одновременно уступают, если второй ресурс занят. Попытки идут, прогресса нет.
 */
public class LivelockDemo {

    private static final int ATTEMPTS = 3;

    public static void main(String[] args) throws Exception {
        ReentrantLock firstTool = new ReentrantLock();
        ReentrantLock secondTool = new ReentrantLock();
        CyclicBarrier bothWorkers = new CyclicBarrier(2);

        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = workers.submit(() -> work(firstTool, secondTool, bothWorkers));
            Future<Integer> second = workers.submit(() -> work(secondTool, firstTool, bothWorkers));
            System.out.println("Попыток: " + 2 * ATTEMPTS + ", работ завершено: " + (first.get() + second.get()));
        } finally {
            workers.shutdownNow();
        }
    }

    private static int work(ReentrantLock ownTool, ReentrantLock otherTool, CyclicBarrier bothWorkers)
            throws Exception {
        int completed = 0;
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            ownTool.lockInterruptibly();
            try {
                // Барьеры воспроизводят неудачное расписание. Оба держат свой инструмент,
                // пока каждый не попробует взять чужой.
                bothWorkers.await(5, TimeUnit.SECONDS);
                if (otherTool.tryLock()) {
                    try {
                        completed++;
                    } finally {
                        otherTool.unlock();
                    }
                }
                bothWorkers.await(5, TimeUnit.SECONDS);
            } finally {
                ownTool.unlock();
            }
        }
        return completed;
    }
}
