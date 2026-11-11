package ru.learn.cuncurrent.coordination;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Упорядочивает три этапа обработки заказа в разных потоках.
 * CountDownLatch запоминает завершение этапа, даже если следующий поток ещё не начал ждать.
 */
public class OrderedStepsDemo {

    public static void main(String[] args) throws Exception {
        CountDownLatch packed = new CountDownLatch(1);
        CountDownLatch paid = new CountDownLatch(1);

        ExecutorService workers = Executors.newFixedThreadPool(3);
        try {
            // Подаём задания в обратном порядке. Порядок работы всё равно задаётся зависимостями.
            Future<Void> ship = workers.submit(() -> {
                paid.await();
                System.out.println("3. Заказ передан курьеру");
                return null;
            });
            Future<Void> pay = workers.submit(() -> {
                packed.await();
                System.out.println("2. Заказ оплачен");
                paid.countDown(); // Сигнал отправляем после успешного завершения этапа.
                return null;
            });
            Future<?> pack = workers.submit(() -> {
                System.out.println("1. Заказ упакован");
                packed.countDown();
            });

            // Проверяем результаты от первого этапа к последнему, чтобы увидеть ошибку в источнике.
            pack.get();
            pay.get();
            ship.get();
        } finally {
            workers.shutdownNow();
        }
    }
}
