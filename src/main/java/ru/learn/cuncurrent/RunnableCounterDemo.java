package ru.learn.cuncurrent;

public class RunnableCounterDemo {

    public static void main(String[] args) {
        RunnableCounterWorker a = new RunnableCounterWorker("A", 10);
        RunnableCounterWorker b = new RunnableCounterWorker("B", 10);

        Thread at = new Thread(a);
        Thread bt = new Thread(b);
        at.start();
        bt.start();
    }
}
