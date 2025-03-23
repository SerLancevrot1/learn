 package ru.learn.cuncurrent;

 import java.util.concurrent.locks.Condition;
 import java.util.concurrent.locks.Lock;
 import java.util.concurrent.locks.ReentrantLock;

 public class FooSafe {

    private static final Lock lock = new ReentrantLock();

    private static final Condition firstMethodCalled = lock.newCondition();

    private static final Condition secondMethodCalled = lock.newCondition();

    public static void first() {
        lock.lock();
        firstMethodCalled.signal();
        System.out.println("first");
        lock.unlock();
    }

    public static void second() {
        lock.lock();
        try {
            firstMethodCalled.await();
            System.out.println("second");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            secondMethodCalled.signal();
            lock.unlock();
        }
    }

    public static void third () {
        lock.lock();
        try {
            secondMethodCalled.await();
            System.out.println("third");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}
