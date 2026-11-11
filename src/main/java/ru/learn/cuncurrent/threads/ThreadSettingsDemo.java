package ru.learn.cuncurrent.threads;

import java.util.concurrent.CountDownLatch;

/**
 * Показывает приоритет и статус daemon у платформенных потоков.
 * Приоритет служит подсказкой планировщику. Daemon-поток сам по себе не удерживает JVM.
 */
public class ThreadSettingsDemo {

    public static void main(String[] args) throws InterruptedException {
        Runnable printPriority = () -> {
            Thread current = Thread.currentThread();
            System.out.printf("%s, приоритет %d%n", current.getName(), current.getPriority());
        };
        Thread high = new Thread(printPriority, "high-priority");
        Thread low = new Thread(printPriority, "low-priority");
        high.setPriority(Thread.MAX_PRIORITY);
        low.setPriority(Thread.MIN_PRIORITY);
        high.start();
        low.start();
        high.join();
        low.join();

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch shutdown = new CountDownLatch(1);
        Thread metrics = new Thread(() -> {
            System.out.println("Daemon-поток ждёт сигнал остановки");
            started.countDown();
            try {
                shutdown.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "metrics");

        metrics.setDaemon(true); // Устанавливается до start().
        metrics.start();
        started.await();
        System.out.println("main выходит, а daemon-поток всё ещё жив: " + metrics.isAlive());
    }
}
