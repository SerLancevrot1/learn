 package ru.learn.cuncurrent;

 import java.util.concurrent.Semaphore;
 import java.util.concurrent.locks.Condition;
 import java.util.concurrent.locks.Lock;
 import java.util.concurrent.locks.ReentrantLock;

 public class FooSemaphoreSafe {

     private final static Semaphore betweenFirstAndSecond = new Semaphore(0);
     private final static Semaphore betweenSecondAndThird = new Semaphore(0);

    public static void first() {
        betweenFirstAndSecond.release();
        System.out.println("first");
    }

    public static void second() {
        try {
            betweenFirstAndSecond.acquire();
            System.out.println("second");
            betweenSecondAndThird.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void third () {
        try {
            betweenSecondAndThird.acquire();
            System.out.println("third");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
