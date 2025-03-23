package ru.learn.cuncurrent;

public class FooSafeDemo {
// ПОРЯДОК ГАРАНТИРУЕТСЯ, ИНОГДА ВЫВОДИТСЯ ТОЛЬКО ПЕРВЫЙ
    public static void main(String[] args) {
        Thread t1 = new Thread(FooSafe::first);
        Thread t2 = new Thread(FooSafe::second);
        Thread t3 = new Thread(FooSafe::third);

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
