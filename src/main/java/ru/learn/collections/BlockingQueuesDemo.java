package ru.learn.collections;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Ограниченный буфер заданий и реакция производителя на перегрузку. */
public class BlockingQueuesDemo {
    public static void main(String[] args) throws Exception {
        backpressure(new ArrayBlockingQueue<>(2));
        backpressure(new LinkedBlockingQueue<>(2));
        producerConsumer();
        retryAtFront();
    }

    private static void backpressure(BlockingQueue<String> jobs) throws InterruptedException {
        jobs.add("Сформировать дневной отчёт");
        jobs.add("Сформировать недельный отчёт");

        // Потребителя пока нет, поэтому результаты отказа и таймаута воспроизводимы.
        System.out.println(jobs.getClass().getSimpleName());
        System.out.println("Принят месячный отчёт: " + jobs.offer("Сформировать месячный отчёт"));
        boolean accepted = jobs.offer("Сформировать квартальный отчёт", 20, TimeUnit.MILLISECONDS);
        System.out.println("Принят квартальный отчёт за время ожидания: " + accepted);
        System.out.println("Освободили место: " + jobs.take());
        System.out.println("Повторная постановка: " + jobs.offer("Сформировать месячный отчёт"));
    }

    private static void producerConsumer() throws Exception {
        BlockingQueue<Report> jobs = new ArrayBlockingQueue<>(1);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> processed = worker.submit(() -> {
                int count = 0;
                Report report;
                while ((report = jobs.take()) != Report.STOP) {
                    System.out.println("Готов отчёт: " + report);
                    count++;
                }
                return count;
            });

            // Поток main — производитель. Если буфер занят, offer ждёт до указанного таймаута.
            // STOP ставится после всех отчётов и завершает единственного потребителя.
            for (Report report : List.of(Report.DAILY, Report.WEEKLY, Report.MONTHLY, Report.STOP)) {
                if (!jobs.offer(report, 5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Очередь не освободилась для " + report);
                }
            }
            System.out.println("Обработано отчётов: " + processed.get(5, TimeUnit.SECONDS));
        } finally {
            worker.shutdownNow();
        }
    }

    private static void retryAtFront() throws InterruptedException {
        BlockingDeque<String> jobs = new LinkedBlockingDeque<>(2);
        jobs.putLast("Отправить новое письмо");
        jobs.putFirst("Повторить отправку после сбоя");
        System.out.println("Первая отправка: " + jobs.takeFirst());
        System.out.println("Следующая отправка: " + jobs.takeFirst());
    }

    private enum Report { DAILY, WEEKLY, MONTHLY, STOP }
}
