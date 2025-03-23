package ru.learn.cuncurrent;

public class ThreadCounterJoinDemo {

    public static void main(String[] args) {
        ThreadCounterWorker a = new ThreadCounterWorker("A", 1000);
        ThreadCounterWorker b = new ThreadCounterWorker("B", 1000);

        a.start();
        b.start();

        try {
            a.join();
            b.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Process is finish");
    }
}
