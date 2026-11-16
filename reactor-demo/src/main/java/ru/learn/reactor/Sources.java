package ru.learn.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

final class Sources {
    private Sources() {
    }

    static Mono<Integer> assemblyValue(IntSupplier read) {
        // Java сначала вызывает read, потом передаёт готовое число в just.
        // Даже без подписки чтение уже произошло. Повторная подписка получит то же число.
        return Mono.just(read.getAsInt());
    }

    static Mono<Integer> deferredValue(IntSupplier read) {
        // Теперь чтение произойдёт во время обработки подписки.
        // Если чтение выбросит, например, IllegalStateException, подписчик получит onError.
        return Mono.fromSupplier(read::getAsInt);
    }

    static Mono<String> displayName(String cached, Supplier<Mono<String>> loadFromStorage) {
        // null превращается в успешное завершение без значения.
        // defer вызывает фабрику резервного источника только при подписке на этот источник.
        return Mono.justOrEmpty(cached)
                .switchIfEmpty(Mono.defer(loadFromStorage));
    }

    static Flux<Integer> countdown(int start) {
        // generate создаёт состояние отдельно для каждой подписки.
        // За один вызов генератора можно передать максимум один элемент.
        // Пока спроса нет, генератор не вызывается.
        return Flux.generate(() -> start, (remaining, sink) -> {
            if (remaining <= 0) {
                sink.complete();
            } else {
                sink.next(remaining);
                if (remaining == 1) {
                    sink.complete();
                }
            }
            return remaining - 1;
        });
    }
}
