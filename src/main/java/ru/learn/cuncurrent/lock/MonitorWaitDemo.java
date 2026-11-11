package ru.learn.cuncurrent.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;

/**
 * Показывает передачу результата через монитор и пару wait()/notifyAll().
 * Состояние хранится в поле, поэтому получатель может прийти и после сигнала.
 */
public class MonitorWaitDemo {

    private final CountDownLatch recipientReady = new CountDownLatch(1);
    private String parcel;

    public static void main(String[] args) throws Exception {
        MonitorWaitDemo mailbox = new MonitorWaitDemo();
        FutureTask<String> pickup = new FutureTask<>(mailbox::waitForParcel);
        new Thread(pickup, "recipient").start();

        try {
            // Только для примера. Доставщик придёт, когда получатель уже держит монитор.
            mailbox.recipientReady.await();
            mailbox.deliver("Посылка №42");
            System.out.println("Получена: " + pickup.get());
        } finally {
            pickup.cancel(true);
        }
    }

    private synchronized String waitForParcel() throws InterruptedException {
        recipientReady.countDown();
        while (parcel == null) {
            wait(); // Временно отпускает монитор this, чтобы доставщик мог войти в deliver().
        }
        return parcel;
    }

    private synchronized void deliver(String deliveredParcel) {
        parcel = deliveredParcel;
        notifyAll(); // Сначала меняем состояние, затем будим ожидающих.
    }
}
