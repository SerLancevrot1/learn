package ru.learn.cuncurrent;

import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class PongWorker extends Thread{

    private final AtomicInteger counter;

    private final Exchanger<AtomicInteger> exchanger;

    @SneakyThrows
    @Override
    public void run() {
        while (true) {
            AtomicInteger exchangeCounter = exchanger.exchange(counter);
            System.out.println("PONG: " + exchangeCounter.getAndIncrement());
        }
    }

}
