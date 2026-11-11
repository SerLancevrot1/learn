package ru.learn.cuncurrent.lock;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Воспроизводит deadlock. Две операции берут одни и те же мониторы в разном порядке.
 * JVM обнаруживает цикл ожидания. Потоки намеренно daemon, чтобы пример мог завершиться.
 */
public class MonitorDeadlockDemo {

    public static void main(String[] args) throws InterruptedException {
        Object orders = new Object();
        Object stock = new Object();
        CountDownLatch firstLocksTaken = new CountDownLatch(2);

        // Чтобы убрать цикл в рабочем коде, обе операции должны брать мониторы в одном порядке.
        startWorker("заказ", orders, stock, firstLocksTaken);
        startWorker("возврат", stock, orders, firstLocksTaken);

        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        long started = System.nanoTime();
        while (System.nanoTime() - started < TimeUnit.SECONDS.toNanos(5)) {
            long[] deadlocked = threads.findMonitorDeadlockedThreads();
            if (deadlocked != null) {
                for (ThreadInfo thread : threads.getThreadInfo(deadlocked)) {
                    System.out.printf("%s ждёт монитор потока %s%n",
                            thread.getThreadName(), thread.getLockOwnerName());
                }
                return;
            }
            TimeUnit.MILLISECONDS.sleep(10); // Интервал опроса диагностики.
        }
        throw new IllegalStateException("JVM не обнаружила цикл ожидания за 5 секунд");
    }

    private static void startWorker(String name, Object first, Object second, CountDownLatch firstLocksTaken) {
        Thread worker = new Thread(() -> {
            synchronized (first) {
                firstLocksTaken.countDown();
                try {
                    // Ждём, пока оба потока удерживают по одному монитору.
                    firstLocksTaken.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                synchronized (second) {
                    System.out.println(name + " получил оба монитора");
                }
            }
        }, name);
        worker.setDaemon(true);
        worker.start();
    }
}
