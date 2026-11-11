package ru.learn.cuncurrent.shared;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Два кассира обновляют общее число продаж. Сравниваем потерю обновления,
 * synchronized, AtomicInteger и LongAdder на одной операции.
 */
public class CounterDemo {

    public static void main(String[] args) throws InterruptedException {
        Counter counter = new Counter();
        runInTwoThreads(counter::incrementWithLostUpdate);
        System.out.println("Чтение + запись без общей блокировки: " + counter.unsafeSales);

        runInTwoThreads(counter::incrementUnderMonitor);
        System.out.println("synchronized: " + counter.monitorSales);

        AtomicInteger atomicSales = new AtomicInteger();
        runInTwoThreads(atomicSales::incrementAndGet);
        System.out.println("AtomicInteger: " + atomicSales.get());

        // LongAdder подходит для статистики. Считываем сумму после завершения всех записей.
        LongAdder totalSales = new LongAdder();
        runInTwoThreads(totalSales::increment);
        System.out.println("LongAdder: " + totalSales.sum());
    }

    private static void runInTwoThreads(Runnable sale) throws InterruptedException {
        Thread firstCashier = new Thread(sale, "cashier-1");
        Thread secondCashier = new Thread(sale, "cashier-2");
        firstCashier.start();
        secondCashier.start();
        firstCashier.join();
        secondCashier.join();
    }

    /** Хранит два счётчика для сравнения опасного и защищённого обновлений. */
    private static class Counter {
        private final Object monitor = new Object();
        private final CountDownLatch bothHaveRead = new CountDownLatch(2);
        private int unsafeSales;
        private int monitorSales;

        private void incrementWithLostUpdate() {
            int previousSales = unsafeSales;
            bothHaveRead.countDown();
            try {
                // Только для демонстрации. Оба кассира уже прочитали ноль и запишут единицу.
                bothHaveRead.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            unsafeSales = previousSales + 1;
        }

        private void incrementUnderMonitor() {
            synchronized (monitor) {
                // Один и тот же монитор защищает всю операцию чтения, сложения и записи.
                // У synchronized-метода монитором был бы this, у статического — объект класса.
                monitorSales++;
            }
        }
    }
}
