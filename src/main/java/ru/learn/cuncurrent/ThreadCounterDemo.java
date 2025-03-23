package ru.learn.cuncurrent;

public class ThreadCounterDemo {

    public static void main(String[] args) {
        ThreadCounterWorker a = new ThreadCounterWorker("A", 1000);
        ThreadCounterWorker b = new ThreadCounterWorker("B", 1000);

        a.start();
        b.start();
    }
}
