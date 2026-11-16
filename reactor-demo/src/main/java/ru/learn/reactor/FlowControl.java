package ru.learn.reactor;

import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.function.Consumer;

final class FlowControl {
    private FlowControl() {
    }

    static void takeInBatches(Consumer<String> log) {
        // Источник умеет ждать спроса. limitRate дробит крупные запросы вверх по цепочке.
        // При lowTide = 0 следующая партия запрашивается после исчерпания предыдущей.
        Flux.range(1, 10)
                .doOnRequest(n -> log.accept("Запрос источнику: " + n))
                .doOnCancel(() -> log.accept("Источник получил cancel"))
                .limitRate(2, 0)
                .take(4)
                .subscribe(new BaseSubscriber<>() {
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        request(2);
                    }

                    @Override
                    protected void hookOnNext(Integer value) {
                        log.accept("Значение: " + value);
                        if (value == 2) {
                            request(2);
                        }
                    }

                    @Override
                    protected void hookOnComplete() {
                        // take отправляет cancel источнику и onComplete своему подписчику.
                        log.accept("Подписчик получил onComplete");
                    }
                });
    }

    static Flux<Long> ticksDroppingWhenBusy(Consumer<Long> dropped) {
        // Тики привязаны к часам. Здесь допустимо пропустить тик, который уже устарел.
        // Оператор принимает тики от interval и смотрит, есть ли спрос ниже него.
        return Flux.interval(Duration.ofSeconds(1)).onBackpressureDrop(dropped);
    }
}
