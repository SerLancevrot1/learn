package ru.learn.cuncurrent.lock;

import java.util.concurrent.atomic.AtomicInteger;

public class StarvationWorker extends Thread {

    private final AtomicInteger atomicInteger;

    public StarvationWorker(AtomicInteger atomicInteger, String name) {
        this.atomicInteger = atomicInteger;
        super.setName(name);
    }

    @Override
    public void run() {

        System.out.println(getName() + " " + atomicInteger.incrementAndGet());
    }
}
