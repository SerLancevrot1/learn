package ru.learn.cuncurrent.threads;

import java.util.concurrent.CountDownLatch;

/**
 * Показывает кооперативную остановку фоновой задачи через interrupt().
 * Прерывание будит ожидающий поток, чтобы тот мог закончить работу и очистить ресурсы.
 */
public class InterruptDemo {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch nextJob = new CountDownLatch(1);
        Thread worker = new Thread(() -> {
            try {
                System.out.println("Рабочий поток ждёт задание");
                started.countDown();
                nextJob.await();
            } catch (InterruptedException e) {
                // await() сбрасывает флаг при выбрасывании исключения. Восстанавливаем его.
                Thread.currentThread().interrupt();
                System.out.println("Остановка запрошена, флаг восстановлен");
            } finally {
                System.out.println("Рабочий поток завершает работу");
            }
        }, "job-worker");

        worker.start();
        try {
            started.await();
        } finally {
            worker.interrupt();
        }
        worker.join();
    }
}
