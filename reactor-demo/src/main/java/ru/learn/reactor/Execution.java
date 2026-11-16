package ru.learn.reactor;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.function.Consumer;

final class Execution {
    static final String TRACE_ID = "traceId";

    private Execution() {
    }

    static Mono<String> readLegacy(Scheduler blockingIo, Scheduler processing, Consumer<String> log) {
        // Обёртка откладывает вызов, а subscribeOn переносит его на выделенный Scheduler.
        return Mono.fromCallable(() -> {
                    log.accept("Чтение выполняется на " + Thread.currentThread().getName());
                    // Имитируем клиент, который удерживает поток в ожидании результата.
                    Thread.sleep(20);
                    return "  товар  ";
                })
                .subscribeOn(blockingIo)
                // После publishOn следующий map выполняется на другом Scheduler.
                .publishOn(processing)
                .map(value -> {
                    log.accept("Обработка выполняется на " + Thread.currentThread().getName());
                    return value.strip();
                });
    }

    static Mono<String> traced(Scheduler processing) {
        // Читаем Context во время подписки. Смена потока его не теряет.
        return Mono.just("готово")
                .publishOn(processing)
                .flatMap(value -> Mono.deferContextual(context ->
                        Mono.just(context.getOrDefault(TRACE_ID, "без traceId") + ": " + value)));
    }
}
