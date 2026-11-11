package ru.learn.cuncurrent.coordination;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Два склада сверяют остатки в два раунда. Каждый начинает следующий раунд,
 * только когда оба дошли до барьера. Один CyclicBarrier используется повторно.
 */
public class CyclicBarrierDemo {

    public static void main(String[] args) throws Exception {
        CyclicBarrier checked = new CyclicBarrier(2, () -> System.out.println("Сверка завершена"));
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Void> north = workers.submit(() -> countStock("Северный склад", checked));
            Future<Void> south = workers.submit(() -> countStock("Южный склад", checked));
            north.get();
            south.get();
        } finally {
            workers.shutdownNow();
        }
    }

    private static Void countStock(String warehouse, CyclicBarrier checked) throws Exception {
        for (int round = 1; round <= 2; round++) {
            System.out.printf("%s, раунд %d: остатки посчитаны%n", warehouse, round);
            // Последний прибывший поток выполнит действие барьера и пропустит обоих дальше.
            checked.await(5, TimeUnit.SECONDS);
        }
        return null;
    }
}
