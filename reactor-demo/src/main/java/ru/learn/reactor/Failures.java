package ru.learn.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

final class Failures {
    private Failures() {
    }

    static Flux<Integer> keepingHealthyItems(Flux<String> input, Consumer<String> rejected) {
        // Внутренняя обработка срабатывает отдельно для каждой строки.
        // При ошибке формата внутренний Mono становится пустым, внешний Flux продолжается.
        return input.concatMap(text -> Mono.fromCallable(() -> Integer.parseInt(text))
                .onErrorResume(NumberFormatException.class, error -> {
                    rejected.accept(text);
                    return Mono.empty();
                }));
    }

    static <T> Mono<T> remoteRead(Mono<T> attempt, Duration attemptTimeout, Duration totalTimeout) {
        return attempt
                // Этот таймер запускается заново на каждой подписке, включая повторную.
                .timeout(attemptTimeout)
                .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(100))
                        .filter(error -> error instanceof TemporaryFailure || error instanceof TimeoutException)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                // Верхний бюджет включает все попытки и ожидание между ними.
                .timeout(totalTimeout);
    }

    static <T> Mono<T> usingSession(Consumer<String> audit, Function<Session, Mono<T>> work) {
        // Новая сессия на подписку. usingWhen управляет асинхронным освобождением ресурса.
        return Mono.usingWhen(
                Mono.fromSupplier(() -> {
                    audit.accept("открыта");
                    return new Session(audit);
                }),
                work,
                session -> session.close("успех"),
                (session, error) -> session.close("ошибка"),
                session -> session.close("отмена"));
    }

    record Session(Consumer<String> audit) {
        Mono<Void> close(String reason) {
            // Для демонстрации закрытие записывается в журнал при подписке на cleanup.
            return Mono.fromRunnable(() -> audit.accept("закрыта: " + reason));
        }
    }

    static final class TemporaryFailure extends RuntimeException {
        TemporaryFailure(String message) {
            super(message);
        }
    }
}
