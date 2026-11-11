package ru.learn.cuncurrent.threads;

/**
 * Показывает разницу между run() и start() и ожидание завершения через join().
 * Runnable описывает работу. Thread определяет, в каком потоке она выполняется.
 */
public class ThreadLifecycleDemo {

    public static void main(String[] args) throws InterruptedException {
        Runnable prepareReport = () ->
                System.out.println("Отчёт готов в потоке " + Thread.currentThread().getName());

        Thread reportWorker = new Thread(prepareReport, "report-worker");
        reportWorker.run();   // Обычный вызов метода. В выводе будет main.
        reportWorker.start(); // JVM запускает новый поток. В выводе будет report-worker.

        reportWorker.join();
        System.out.println("Главный поток дождался отчёта");
    }
}
