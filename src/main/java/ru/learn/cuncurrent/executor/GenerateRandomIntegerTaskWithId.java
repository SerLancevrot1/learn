package ru.learn.cuncurrent.executor;

import java.util.Random;

import lombok.SneakyThrows;

public class GenerateRandomIntegerTaskWithId implements Runnable{

    private final Random random = new Random();

    private final int id;

    public GenerateRandomIntegerTaskWithId(int id) {
        this.id = id;
    }

    @SneakyThrows
    @Override
    public void run() {
        Thread.sleep(100);
        System.out.println("id = " + id + " --- " + random.nextInt(1000));
    }
}
