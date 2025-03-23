package ru.learn.cuncurrent;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class SyncThread implements Runnable{

    private final Object lock1;
    private final Object lock2;

    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        System.out.println("Thread " + name + " acquiring lock on " + lock1);
        synchronized (lock1) {
            System.out.println(name + "acquired " + lock1);
            work();
            System.out.println("Thread " + name + " acquiring lock on " + lock2);
            synchronized (lock2) {
                System.out.println(name + "acquired " + lock2);
                work();
            }
            System.out.println(name + " realise " + lock2);
        }
        System.out.println(name + " realise " + lock1);
        System.out.println(name + " is finish work");

    }

    @SneakyThrows
    private void work() {
        Thread.sleep(5000);
    }
}
