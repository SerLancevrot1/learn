package ru.learn.cuncurrent;

public class FooSemaphoreSafeDemo {

    public static void main(String[] args) {
        Thread t1 = new Thread(FooSemaphoreSafe::first);
        Thread t2 = new Thread(FooSemaphoreSafe::second);
        Thread t3 = new Thread(FooSemaphoreSafe::third);

        t1.start();
        t2.start();
        t3.start();

        try {
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException e) {
            throw new RuntimeException();
        }
    }
}
