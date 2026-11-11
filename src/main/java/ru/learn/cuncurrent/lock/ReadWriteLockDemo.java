package ru.learn.cuncurrent.lock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Показывает чтение согласованной пары цен. Несколько читателей могут
 * держать readLock вместе. Для изменения цен нужен эксклюзивный writeLock.
 */
public class ReadWriteLockDemo {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private int ticketPrice = 100;
    private int baggagePrice = 20;

    public static void main(String[] args) throws Exception {
        ReadWriteLockDemo prices = new ReadWriteLockDemo();
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            Future<Integer> firstQuote = pool.submit(prices::totalPrice);
            Future<?> update = pool.submit(() -> prices.update(150, 30));
            Future<Integer> secondQuote = pool.submit(prices::totalPrice);

            // Каждый запрос вернёт 120 или 180, в зависимости от расписания.
            System.out.println("Первый запрос: " + firstQuote.get());
            System.out.println("Второй запрос: " + secondQuote.get());
            update.get();
            System.out.println("Цена после обновления: " + prices.totalPrice());
        } finally {
            pool.shutdownNow();
        }
    }

    private int totalPrice() {
        lock.readLock().lock();
        try {
            return ticketPrice + baggagePrice;
        } finally {
            lock.readLock().unlock();
        }
    }

    private void update(int ticket, int baggage) {
        lock.writeLock().lock();
        try {
            ticketPrice = ticket;
            baggagePrice = baggage;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
