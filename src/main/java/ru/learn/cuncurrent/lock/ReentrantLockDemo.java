package ru.learn.cuncurrent.lock;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Показывает ограниченное ожидание через tryLock(). Фоновый пересчёт
 * пропускает попытку, пока ресурс занят, и может вернуться к нему позже.
 */
public class ReentrantLockDemo {

    public static void main(String[] args) throws Exception {
        ReentrantLock reportLock = new ReentrantLock();
        reportLock.lock();
        try {
            FutureTask<Boolean> refresh = new FutureTask<>(() -> tryRefresh(reportLock));
            new Thread(refresh, "report-refresh").start();
            // В демонстрации удерживаем lock до результата, чтобы гарантировать таймаут.
            System.out.println("Пересчёт занятого отчёта: " + refresh.get());
        } finally {
            reportLock.unlock();
        }

        System.out.println("Повтор после освобождения: " + tryRefresh(reportLock));
    }

    private static boolean tryRefresh(ReentrantLock reportLock) throws InterruptedException {
        if (!reportLock.tryLock(100, TimeUnit.MILLISECONDS)) {
            return false;
        }
        try {
            System.out.println("Отчёт пересчитан");
            return true;
        } finally {
            // Освобождаем lock только после успешного захвата, даже если работа завершится с ошибкой.
            reportLock.unlock();
        }
    }
}
