package ru.learn.collections;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Очереди задач по приоритету и по моменту готовности. */
public class PriorityQueuesDemo {
    public static void main(String[] args) throws InterruptedException {
        byPriority();
        afterDelay();
    }

    private static void byPriority() throws InterruptedException {
        BlockingQueue<Job> jobs = new PriorityBlockingQueue<>(2, Comparator.comparingInt(Job::priority));
        jobs.put(new Job("Перестроить поисковый индекс", 3));
        jobs.put(new Job("Восстановить платежи", 1));
        // 2 — начальная ёмкость, хотя фактически очередь уже хранит три задания.
        jobs.put(new Job("Сверить остатки на складе", 2));
        System.out.println("Первое задание: " + jobs.take().description());
        System.out.println("Следующее задание: " + jobs.take().description());
        System.out.println("Последнее задание: " + jobs.take().description());
    }

    private static void afterDelay() {
        DelayQueue<Retry> retries = new DelayQueue<>();
        retries.offer(new Retry("Повторить отправку позже", System.nanoTime() + TimeUnit.HOURS.toNanos(1)));
        retries.offer(new Retry("Повторить запрос сейчас", System.nanoTime()));

        // После готовой задачи в очереди остаётся отложенная. Для неё poll пока возвращает null.
        System.out.println("Задание готово: " + retries.remove().description());
        System.out.println("Следующее по сроку: " + retries.element().description());
        System.out.println("Доступно сейчас: " + retries.poll());
        System.out.println("Осталось в очереди: " + retries.size());
    }

    private record Job(String description, int priority) {
    }

    private record Retry(String description, long readyAtNanos) implements Delayed {
        @Override
        public long getDelay(TimeUnit unit) {
            // nanoTime подходит для относительных задержек и не зависит от перевода часов.
            return unit.convert(readyAtNanos - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            // В этой очереди только Retry. Вычитание сравнивает близкие дедлайны даже при переполнении nanoTime.
            return Long.compare(readyAtNanos - ((Retry) other).readyAtNanos, 0);
        }
    }
}
