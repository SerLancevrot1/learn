package ru.learn.cuncurrent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.util.StopWatch;

public class ReentrantLockDemo {

    private static Integer counter = 0;

    //как sychronize, но с расширенными возможностями. Отпустит лок только тот поток, который его захватил
    private static final Lock lock = new ReentrantLock();

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
        lock.lock();
        counter++;
        lock.unlock();
    }
}
