package ru.learn.cuncurrent;

public class RunnableCounterWorker implements Runnable {

    private final String name;
    private final int range;

    public RunnableCounterWorker(String name, int range) {
        this.name = name;
        this.range = range;
    }

    @Override
    public void run() {
        int counter = 0;
        while (counter < range) {
            System.out.println(name + ": " + counter++);
        }
    }
}
