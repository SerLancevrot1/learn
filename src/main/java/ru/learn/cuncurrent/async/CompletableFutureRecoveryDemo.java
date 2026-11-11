package ru.learn.cuncurrent.async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Собирает частичный ответ, когда один сервис вернул ошибку, а второй не ответил.
 * Запасные значения позволяют завершить общий запрос через allOf().
 */
public class CompletableFutureRecoveryDemo {

    public static void main(String[] args) {
        // Готовая ошибка и незавершённый future имитируют два разных ответа сервисов.
        CompletableFuture<String> promotions = CompletableFuture
                .<String>failedFuture(new IllegalStateException("Сервис акций недоступен"))
                .exceptionally(error -> {
                    System.out.println("Причина: " + error.getMessage());
                    return "Акций нет";
                });
        CompletableFuture<String> recommendations = new CompletableFuture<String>()
                .completeOnTimeout("Рекомендаций нет", 100, TimeUnit.MILLISECONDS);

        // completeOnTimeout завершает future запасным значением. orTimeout завершил бы его ошибкой.
        // Оба метода ограничивают ожидание результата без остановки работы, которая его вычисляет.
        // allOf() возвращает Void. Значения читаем из отдельных future после завершения обоих.
        CompletableFuture.allOf(promotions, recommendations).join();
        System.out.println(promotions.join());
        System.out.println(recommendations.join());
    }
}
