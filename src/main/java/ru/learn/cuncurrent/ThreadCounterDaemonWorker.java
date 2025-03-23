package ru.learn.cuncurrent;

public class ThreadCounterDaemonWorker extends Thread {

    private final String name;
    private final int range;

    public ThreadCounterDaemonWorker(String name, int range, boolean daemon) {
        this.name = name;
        this.range = range;
        super.setDaemon(daemon);
    }

    @Override
    public void run() {
        int counter = 0;
        while (counter < range) {
            System.out.println(name + ": " + counter++);
        }
    }
}
