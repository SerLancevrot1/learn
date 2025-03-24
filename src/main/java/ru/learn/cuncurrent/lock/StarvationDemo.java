package ru.learn.cuncurrent.lock;

import java.util.concurrent.atomic.AtomicInteger;

public class StarvationDemo {

    public static void main(String[] args) {
        AtomicInteger atomicInteger = new AtomicInteger();
        StarvationWorker t1 = new StarvationWorker(atomicInteger, "t1");
        t1.setPriority(10);
        StarvationWorker t2 = new StarvationWorker(atomicInteger, "t2");
        t2.setPriority(9);
        StarvationWorker t3 = new StarvationWorker(atomicInteger, "t3");
        t3.setPriority(8);
        StarvationWorker t4 = new StarvationWorker(atomicInteger, "t4");
        t4.setPriority(7);
        // наименьший приоритет
        StarvationWorker t5 = new StarvationWorker(atomicInteger, "t5");
        t5.setPriority(1);

        t1.start();
        t2.start();
        t3.start();
        t4.start();
        // Поток будет ожидать выполнения других потоков без шанса на выполнение. Это явления называется starvation
        t5.start();
    }
}
