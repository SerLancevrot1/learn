package ru.learn.cuncurrent;

import org.springframework.util.StopWatch;

public class IncrementCounterSynchronizeBlockOnObjectDemo {

    private static Integer counter = 0;

    private static final Object counterLock = new Object();

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
        System.out.println(counter);
    }

    private static void increment() {
        synchronized (counterLock) {
            counter++;
        }
    }
}
