package ru.learn.reactor;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.PublisherProbe;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderQuotesTest {
    private static final Duration VERIFY_TIMEOUT = Duration.ofSeconds(5);

    @Test
    void quoteLimitsConcurrentReadsAndPreservesOrder() {
        List<Integer> started = new ArrayList<>();
        List<Integer> completed = new ArrayList<>();
        OrderQuotes service = new OrderQuotes(id -> {
            started.add(id);
            long delay = id == 3 ? 30 : 10;
            return Mono.delay(Duration.ofMillis(delay)).thenReturn(id * 100L)
                    .doOnSuccess(price -> completed.add(id));
        });

        StepVerifier.withVirtualTime(() -> service.calculate(List.of(3, 2, 1), Mono.just(50L))
                        .contextWrite(context -> context.put(Execution.TRACE_ID, "test-request")))
                .expectSubscription()
                .then(() -> assertEquals(List.of(3, 2), started))
                .expectNoEvent(Duration.ofMillis(10))
                .then(() -> {
                    assertEquals(List.of(3, 2), started);
                    assertEquals(List.of(2), completed);
                })
                .thenAwait(Duration.ofMillis(30))
                .expectNext(new OrderQuotes.Quote(List.of(
                        new OrderQuotes.Line(3, 300), new OrderQuotes.Line(2, 200),
                        new OrderQuotes.Line(1, 100)), 550, "test-request"))
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
        assertEquals(List.of(2, 3, 1), completed);
    }

    @Test
    void emptyDiscountUsesZeroAndOnlyFailedReadIsRetried() {
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        OrderQuotes service = new OrderQuotes(id -> {
            int attempts = (id == 1 ? first : second).incrementAndGet();
            return id == 1 && attempts == 1
                    ? Mono.error(new Failures.TemporaryFailure("занято"))
                    : Mono.just(id * 100L);
        });

        StepVerifier.withVirtualTime(() -> service.calculate(List.of(1, 2), Mono.empty()))
                .thenAwait(Duration.ofMillis(100))
                .expectNext(new OrderQuotes.Quote(List.of(
                        new OrderQuotes.Line(1, 100), new OrderQuotes.Line(2, 200)), 300, "без traceId"))
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
        assertEquals(2, first.get());
        assertEquals(1, second.get());
    }

    @Test
    void missingPriceFailsQuoteAndCancelsOtherRead() {
        PublisherProbe<Long> pendingPrice = PublisherProbe.of(Mono.never());
        OrderQuotes service = new OrderQuotes(id -> id == 1 ? pendingPrice.mono() : Mono.empty());
        StepVerifier.create(service.calculate(List.of(1, 2), Mono.empty()))
                .expectErrorMessage("Нет цены для товара 2")
                .verify(VERIFY_TIMEOUT);
        pendingPrice.assertWasCancelled();
    }
}
