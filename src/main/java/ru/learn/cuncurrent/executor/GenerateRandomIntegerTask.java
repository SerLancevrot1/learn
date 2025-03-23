package ru.learn.cuncurrent.executor;

import java.util.Random;

import lombok.SneakyThrows;

public class GenerateRandomIntegerTask implements Runnable{

    private final Random random = new Random();

    @SneakyThrows
    @Override
    public void run() {
        Thread.sleep(100);
        System.out.println("GenerateRandomIntegerTask " + random.nextInt(1000));
    }
}
