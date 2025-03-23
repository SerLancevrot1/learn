package ru.learn.cuncurrent;

public class ThreadCounterWorker extends Thread {

    private final String name;
    private final int range;

    public ThreadCounterWorker(String name, int range) {
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
