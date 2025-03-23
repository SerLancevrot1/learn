package ru.learn.cuncurrent;

import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicInteger;

public class ExchangerDemo {
    // Exchanger позволяет обмениваться информацией, но синхронизацию должны обеспечить мы сами
    public static void main(String[] args) {
        AtomicInteger counter = new AtomicInteger();

        Exchanger<AtomicInteger> exchanger = new Exchanger<>();

        PingWorker pingWorker = new PingWorker(counter, exchanger);
        PongWorker pongWorker = new PongWorker(counter, exchanger);

        pingWorker.start();
        pongWorker.start();
    }
}
