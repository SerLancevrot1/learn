package ru.learn.cuncurrent.coordination;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Показывает producer-consumer. Один поток ставит отчёты в ограниченную очередь,
 * второй забирает их. Специальное сообщение завершает обработку.
 */
public class BlockingQueueDemo {

    /** Сообщения, которыми обмениваются производитель и потребитель. */
    private enum Job { DAILY_REPORT, WEEKLY_REPORT, MONTHLY_REPORT, STOP }

    public static void main(String[] args) throws Exception {
        BlockingQueue<Job> jobs = new ArrayBlockingQueue<>(2);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Void> producer = workers.submit(() -> {
                jobs.put(Job.DAILY_REPORT);
                jobs.put(Job.WEEKLY_REPORT);
                jobs.put(Job.MONTHLY_REPORT); // При полной очереди put() ждёт свободного места.
                jobs.put(Job.STOP);
                return null;
            });

            Future<Integer> consumer = workers.submit(() -> {
                int processed = 0;
                Job job;
                while ((job = jobs.take()) != Job.STOP) { // При пустой очереди take() ждёт данные.
                    System.out.println("Готов отчёт: " + job);
                    processed++;
                }
                return processed;
            });
            // Таймауты позволяют остановить оба потока, если один из них перестал выполнять свою роль.
            producer.get(5, TimeUnit.SECONDS);
            System.out.println("Обработано отчётов: " + consumer.get(5, TimeUnit.SECONDS));
        } finally {
            workers.shutdownNow();
        }
    }
}
