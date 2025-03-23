package ru.learn.cuncurrent;

public class ThreadCounterDaemonDemo {

    public static void main(String[] args) {
        ThreadCounterDaemonWorker a = new ThreadCounterDaemonWorker("A", 1000, true);
        ThreadCounterDaemonWorker b = new ThreadCounterDaemonWorker("B", 100, false);

        a.start();
        b.start();
    }
}
