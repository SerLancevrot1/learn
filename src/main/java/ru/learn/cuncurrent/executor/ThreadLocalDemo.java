package ru.learn.cuncurrent.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Один поток пула обслуживает несколько запросов. ThreadLocal хранит контекст
 * текущего запроса, а remove() очищает его перед повторным использованием потока.
 */
public class ThreadLocalDemo {

    public static void main(String[] args) throws Exception {
        ThreadLocal<String> requestId = new ThreadLocal<>();
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            worker.submit(() -> {
                requestId.set("request-42");
                try {
                    System.out.println("Обрабатывается " + requestId.get());
                } finally {
                    requestId.remove(); // Без очистки следующий запрос увидит чужое значение.
                }
            }).get();
            worker.submit(() -> {
                System.out.println("Контекст следующего запроса: " + requestId.get());
            }).get();
        } finally {
            worker.shutdownNow();
        }
    }
}
