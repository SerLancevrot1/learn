package ru.learn.cuncurrent.shared;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Показывает видимость между потоками. Запись в volatile-флаг публикует
 * также результат, записанный перед ним.
 */
public class VolatileFlagDemo {

    private String report;
    private volatile boolean ready;

    public static void main(String[] args) throws InterruptedException {
        VolatileFlagDemo demo = new VolatileFlagDemo();
        CountDownLatch started = new CountDownLatch(1);
        Thread reader = new Thread(() -> {
            started.countDown();
            while (!demo.ready) {
                // Пауза снижает нагрузку при опросе. Видимость обеспечивает volatile.
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
            }
            System.out.println("Прочитан результат: " + demo.report);
        }, "report-reader");

        reader.start();
        try {
            started.await();
        } finally {
            demo.report = "Продано 42 билета";
            demo.ready = true;
        }
        reader.join();
    }
}
