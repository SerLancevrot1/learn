package ru.learn.cuncurrent;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.util.StopWatch;

public class IncrementCounterAtomicDemo {

    private static AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Thread a = new Thread(() -> {
            for (int i = 0; i < 1_000_000; i++) {
                increment();
            }
        });

        Thread b = new Thread(() -> {
            for (int i = 0; i < 1_000_000; i++) {
                increment();
            }
        });

        a.start();
        b.start();

        try {
            a.join();
            b.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeSeconds());
        System.out.println(counter.get());
    }

    private static synchronized void increment() {
        counter.incrementAndGet();
    }
}
