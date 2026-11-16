package ru.learn.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

final class Composition {
    private Composition() {
    }

    static Flux<String> labels(Flux<Integer> values) {
        // filter решает, пропустить ли число дальше. map сразу строит из него строку.
        return values.filter(value -> value % 2 == 0)
                .map(value -> "item-" + value);
    }

    static Mono<String> summary(Mono<String> name, Mono<Integer> stock) {
        // Для zip нужны два значения. Пустой Mono приводит к пустому результату.
        // Если второй вызов зависит от name, связываем их через flatMap.
        return Mono.zip(name, stock, (product, count) -> product + ": " + count);
    }

    static Mono<String> afterSave(Mono<Void> save, Mono<String> response) {
        // save сообщает только о завершении. then подписывается на response после успеха save.
        // Если save завершится ошибкой, цепочка передаст ошибку дальше.
        return save.then(response);
    }

    static Mono<String> lookup(int id) {
        // Условный сетевой вызов. Первый результат готов через 30 мс,
        // второй через 20 мс, третий через 10 мс с момента своей подписки.
        return Mono.delay(Duration.ofMillis(40 - id * 10L)).thenReturn("item-" + id);
    }

    static Flux<String> interleaved() {
        // Максимум три активные подписки на lookup, выдача по готовности.
        return Flux.just(1, 2, 3).flatMap(Composition::lookup, 3);
    }

    static Flux<String> sequential() {
        // Следующий lookup начнётся после завершения предыдущего.
        return Flux.just(1, 2, 3).concatMap(Composition::lookup);
    }

    static Flux<String> orderedConcurrent() {
        // Вызовы запускаются заранее. Готовые результаты ждут своей очереди выдачи.
        return Flux.just(1, 2, 3).flatMapSequential(Composition::lookup, 3);
    }
}
