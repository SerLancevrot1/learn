package ru.learn.cuncurrent.coordination;

import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Две задачи передают друг другу готовые отчёты. Каждый вызов exchange()
 * ждёт партнёра и возвращает значение, которое тот передал.
 */
public class ExchangerDemo {

    public static void main(String[] args) throws Exception {
        Exchanger<String> reports = new Exchanger<>();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<String> sales = workers.submit(() -> reports.exchange("Продажи", 5, TimeUnit.SECONDS));
            Future<String> stock = workers.submit(() -> reports.exchange("Остатки", 5, TimeUnit.SECONDS));
            System.out.println("Отдел продаж получил: " + sales.get());
            System.out.println("Склад получил: " + stock.get());
        } finally {
            workers.shutdownNow();
        }
    }
}
