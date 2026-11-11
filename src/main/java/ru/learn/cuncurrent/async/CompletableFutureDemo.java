package ru.learn.cuncurrent.async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Собирает итоговую стоимость заказа. Зависимые шаги образуют цепочку,
 * а независимый запрос тарифа доставки запускается отдельно.
 */
public class CompletableFutureDemo {

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<Integer> delivery = CompletableFuture.supplyAsync(() -> 300, pool);

            CompletableFuture<Integer> goods = CompletableFuture.supplyAsync(() -> "order-42", pool)
                    // thenCompose объединяет результат с новым асинхронным запросом в одну цепочку.
                    .thenCompose(orderId -> loadSubtotal(orderId, pool))
                    // thenApply преобразует готовое значение, здесь применяет скидку 10%.
                    .thenApply(subtotal -> subtotal * 9 / 10);

            // thenCombine ждёт результаты двух независимых веток и складывает их.
            CompletableFuture<Integer> total = goods.thenCombine(delivery, Integer::sum);
            // Async явно отправляет действие в указанный пул. join() ждёт его на границе программы.
            total.thenAcceptAsync(amount -> System.out.println("Заказ с доставкой: " + amount), pool)
                    .join();
        } finally {
            pool.shutdownNow();
        }
    }

    private static CompletableFuture<Integer> loadSubtotal(String orderId, ExecutorService pool) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("Загружена стоимость для " + orderId);
            return 1200;
        }, pool);
    }
}
