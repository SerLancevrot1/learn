package ru.learn.cuncurrent;

public class FooUnsafeDemo {
// ПОРЯДОК НЕ ГАРАНТИРУЕТСЯ
    public static void main(String[] args) {
        Thread t1 = new Thread(FooUnsafe::first);
        Thread t2 = new Thread(FooUnsafe::second);
        Thread t3 = new Thread(FooUnsafe::third);

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
