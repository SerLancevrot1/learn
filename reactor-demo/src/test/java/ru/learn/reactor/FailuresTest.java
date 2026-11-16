package ru.learn.reactor;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.PublisherProbe;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FailuresTest {
    private static final Duration VERIFY_TIMEOUT = Duration.ofSeconds(5);

    @Test
    void recoveryInsideConcatMapKeepsProcessingOtherItems() {
        List<String> rejected = new ArrayList<>();
        StepVerifier.create(Failures.keepingHealthyItems(Flux.just("1", "x", "3"), rejected::add))
                .expectNext(1, 3)
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
        assertEquals(List.of("x"), rejected);
    }

    @Test
    void retryMakesNewSubscriptionsUntilSuccess() {
        AtomicInteger attempts = new AtomicInteger();
        StepVerifier.withVirtualTime(() -> Failures.remoteRead(
                        Mono.defer(() -> attempts.incrementAndGet() < 3
                                ? Mono.error(new Failures.TemporaryFailure("занято"))
                                : Mono.just("ответ")),
                        Duration.ofMillis(500), Duration.ofSeconds(2)))
                .thenAwait(Duration.ofMillis(200))
                .expectNext("ответ")
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
        assertEquals(3, attempts.get());
    }

    @Test
    void permanentFailureDoesNotRetry() {
        PublisherProbe<String> attempt = PublisherProbe.of(Mono.error(new IllegalArgumentException("id")));
        StepVerifier.create(Failures.remoteRead(attempt.mono(), Duration.ofMillis(500), Duration.ofSeconds(2)))
                .expectError(IllegalArgumentException.class)
                .verify(VERIFY_TIMEOUT);
        assertEquals(1L, attempt.subscribeCount());
    }

    @Test
    void timeoutOnOneAttemptAllowsRetryWithinTotalBudget() {
        AtomicInteger attempts = new AtomicInteger();
        StepVerifier.withVirtualTime(() -> Failures.remoteRead(
                        Mono.defer(() -> attempts.incrementAndGet() == 1 ? Mono.never() : Mono.just("ответ")),
                        Duration.ofMillis(400), Duration.ofSeconds(2)))
                .expectSubscription()
                .expectNoEvent(Duration.ofMillis(499))
                .thenAwait(Duration.ofMillis(1))
                .expectNext("ответ")
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
        assertEquals(2, attempts.get());
    }

    @Test
    void totalTimeoutCancelsSecondAttemptBeforeItCanTimeOutIndividually() {
        PublisherProbe<String> attempt = PublisherProbe.of(Mono.never());
        StepVerifier.withVirtualTime(() -> Failures.remoteRead(
                        attempt.mono(), Duration.ofMillis(400), Duration.ofMillis(600)))
                .thenAwait(Duration.ofMillis(600))
                .expectError(TimeoutException.class)
                .verify(VERIFY_TIMEOUT);
        assertEquals(2L, attempt.subscribeCount());
        attempt.assertWasCancelled();
    }

    @Test
    void sessionClosesBeforeSuccessIsDelivered() {
        List<String> audit = new ArrayList<>();
        StepVerifier.create(Failures.usingSession(audit::add, session -> Mono.just("результат")))
                .assertNext(value -> {
                    assertEquals("результат", value);
                    assertEquals(List.of("открыта", "закрыта: успех"), audit);
                })
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
    }

    @Test
    void sessionClosesOnError() {
        List<String> audit = new ArrayList<>();
        StepVerifier.create(Failures.usingSession(audit::add, session -> Mono.error(new IllegalStateException())))
                .expectError(IllegalStateException.class)
                .verify(VERIFY_TIMEOUT);
        assertEquals(List.of("открыта", "закрыта: ошибка"), audit);
    }

    @Test
    void sessionClosesOnCancellation() {
        List<String> audit = new ArrayList<>();
        PublisherProbe<String> work = PublisherProbe.of(Mono.never());
        StepVerifier.create(Failures.usingSession(audit::add, session -> work.mono()))
                .then(work::assertWasSubscribed)
                .thenCancel()
                .verify(VERIFY_TIMEOUT);
        work.assertWasCancelled();
        assertEquals(List.of("открыта", "закрыта: отмена"), audit);
    }

    @Test
    void cleanupErrorReplacesSuccessfulValue() {
        IllegalStateException closeFailure = new IllegalStateException("не удалось закрыть");
        StepVerifier.withVirtualTime(() -> Mono.usingWhen(
                        Mono.just("ресурс"),
                        resource -> Mono.just("результат"),
                        resource -> Mono.delay(Duration.ofMillis(10)).then(Mono.error(closeFailure))))
                .expectSubscription()
                .expectNoEvent(Duration.ofMillis(9))
                .thenAwait(Duration.ofMillis(1))
                .expectErrorSatisfies(error -> assertEquals(closeFailure, error.getCause()))
                .verify(VERIFY_TIMEOUT);
    }
}
