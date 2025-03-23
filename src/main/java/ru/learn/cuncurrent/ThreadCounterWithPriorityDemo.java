package ru.learn.cuncurrent;

public class ThreadCounterWithPriorityDemo {

    public static void main(String[] args) {
        ThreadCounterWithPriorityWorker a = new ThreadCounterWithPriorityWorker("A", 15, 10);
        ThreadCounterWithPriorityWorker b = new ThreadCounterWithPriorityWorker("B", 15, 1);

        a.start();
        b.start();
    }
}
