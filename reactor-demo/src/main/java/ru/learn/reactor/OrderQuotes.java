package ru.learn.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

final class OrderQuotes {
    private static final int CONCURRENT_READS_PER_QUOTE = 2;

    private final Function<Integer, Mono<Long>> priceByProductId;

    OrderQuotes(Function<Integer, Mono<Long>> priceByProductId) {
        this.priceByProductId = priceByProductId;
    }

    Mono<Quote> calculate(List<Integer> productIds, Mono<Long> discountInCents) {
        // Вход уже находится в памяти. Делаем снимок, чтобы изменения списка после
        // сборки цепочки не меняли состав заказа при очередной подписке.
        List<Integer> ids = List.copyOf(productIds);
        Mono<List<Line>> lines = Flux.fromIterable(ids)
                // До двух запросов цены на эту подписку. Ответ сохраняет порядок позиций.
                .flatMapSequential(this::loadLine, CONCURRENT_READS_PER_QUOTE)
                // Размер заказа конечен. Поэтому здесь можно собрать позиции в список.
                .collectList();

        // Скидка необязательна. Запрос скидки может идти одновременно с запросами цены.
        // Если он завершится ошибкой, эта ошибка попадёт клиенту.
        return Mono.deferContextual(context -> Mono.zip(
                lines,
                discountInCents.defaultIfEmpty(0L),
                (items, discount) -> {
                    long subtotal = items.stream().mapToLong(Line::priceInCents).sum();
                    return new Quote(items, Math.max(0L, subtotal - discount),
                            context.getOrDefault(Execution.TRACE_ID, "без traceId"));
                }));
    }

    private Mono<Line> loadLine(int id) {
        // При каждой повторной подписке gateway вызывается заново.
        // Повторяем только чтение цены конкретного товара. Остальные позиции продолжают работу.
        return Failures.remoteRead(
                        Mono.defer(() -> priceByProductId.apply(id)),
                        Duration.ofMillis(500),
                        Duration.ofSeconds(2))
                // Отсутствие цены — ошибка всей котировки. Частичный итог вводил бы в заблуждение.
                .switchIfEmpty(Mono.error(() -> new UnknownProduct(id)))
                .map(price -> new Line(id, price));
    }

    record Line(int productId, long priceInCents) {
    }

    record Quote(List<Line> lines, long totalInCents, String traceId) {
    }

    static final class UnknownProduct extends RuntimeException {
        UnknownProduct(int id) {
            super("Нет цены для товара " + id);
        }
    }
}
