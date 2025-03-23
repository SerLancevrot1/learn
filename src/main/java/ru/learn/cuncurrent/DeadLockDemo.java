package ru.learn.cuncurrent;

import lombok.SneakyThrows;

public class DeadLockDemo {

    @SneakyThrows
    public static void main(String[] args) {
        Object o1 = new Object();
        Object o2 = new Object();
        Object o3 = new Object();

        Thread t1 = new Thread(new SyncThread(o1, o2), "t1");
        Thread t2 = new Thread(new SyncThread(o2, o3), "t2");
        Thread t3 = new Thread(new SyncThread(o3, o1), "t3");

        t1.start();
        Thread.sleep(1000);
        t2.start();
        Thread.sleep(1000);
        t3.start();
    }
}
