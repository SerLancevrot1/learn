package ru.learn.cuncurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompletableFutureDemo {

    public static void main(String[] args) {
        ExecutorService executor0 = Executors.newWorkStealingPool();
        ExecutorService executor1 = Executors.newWorkStealingPool();
        //Завершено, когда оба future завершены
        CompletableFuture<String> waitingForAll = CompletableFuture
                .allOf(
                        CompletableFuture.supplyAsync(() -> "first"),
                        CompletableFuture.supplyAsync(() -> "second", executor1)
                )
                .thenApply(ignored -> " is completed.");
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> "Concurrency Refcard", executor0)
                //Использование того же исполнителя
                .thenApply(result -> "Java " + result)
                //Использование другого исполнителя
                .thenApplyAsync(result -> "Dzone " + result, executor1)
                //Завершено, когда это и другое future завершено
                .thenCombine(waitingForAll, (first, second) -> first + second)
                //Неявно использование ForkJoinPool#commonPool как исполнителя
                .thenAcceptAsync(result -> {
                    System.out.println("Result is '" + result + "'.");
                })
                //Общий обработчик
                .whenComplete((ignored, exception) -> {
                    if (exception != null)
                        exception.printStackTrace();
                });
        //Первый блокирующий вызов - блокирует, пока он не будет завершен.
        future.join();
        future
                //Выполняется в текущем потоке (который является основным).
                .thenRun(() -> System.out.println("Current thread is '" + Thread.currentThread().getName() + "'."))
                //Неявное использование ForkJoinPool#commonPool как исполнителя
                .thenRunAsync(() -> System.out.println("Current thread is '" + Thread.currentThread().getName() + "'."));
    }
}
