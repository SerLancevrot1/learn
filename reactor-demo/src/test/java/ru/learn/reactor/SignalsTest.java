package ru.learn.reactor;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.test.publisher.PublisherProbe;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalsTest {
    private static final Duration VERIFY_TIMEOUT = Duration.ofSeconds(5);

    @Test
    void justCapturesValueAndSupplierRunsAgainEachSubscription() {
        AtomicInteger calls = new AtomicInteger();
        Mono<Integer> eager = Sources.assemblyValue(calls::incrementAndGet);
        Mono<Integer> lazy = Sources.deferredValue(calls::incrementAndGet);
        assertEquals(1, calls.get());

        StepVerifier.create(Flux.concat(eager, eager, lazy, lazy))
                .expectNext(1, 1, 2, 3)
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
    }

    @Test
    void emptyTriggersFallbackAndValueSkipsIt() {
        PublisherProbe<String> fallback = PublisherProbe.of(Mono.just("из базы"));
        AtomicInteger factories = new AtomicInteger();

        StepVerifier.create(Sources.displayName("из кеша", () -> {
                    factories.incrementAndGet();
                    return fallback.mono();
                }))
                .expectNext("из кеша")
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
        assertEquals(0, factories.get());
        fallback.assertWasNotSubscribed();

        StepVerifier.create(Sources.displayName(null, fallback::mono))
                .expectNext("из базы")
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
        fallback.assertWasSubscribed();
    }

    @Test
    void generateStartsFromInitialStateForEachSubscription() {
        Flux<Integer> numbers = Sources.countdown(3);
        StepVerifier.create(Flux.concat(numbers, numbers), 0)
                .thenRequest(1)
                .expectNext(3)
                .thenRequest(5)
                .expectNext(2, 1, 3, 2, 1)
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
    }

    @Test
    void emptyZipSkipsCombinatorAndThenWaitsForSuccessfulCompletion() {
        StepVerifier.create(Composition.summary(Mono.empty(), Mono.just(5)))
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
        StepVerifier.create(Composition.afterSave(Mono.empty(), Mono.just("сохранено")))
                .expectNext("сохранено")
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
    }

    @Test
    void flatMapLetsFastCallsFinishFirst() {
        // Publisher создаётся внутри Supplier, когда виртуальный Scheduler уже установлен.
        StepVerifier.withVirtualTime(Composition::interleaved)
                .thenAwait(Duration.ofMillis(30))
                .expectNext("item-3", "item-2", "item-1")
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
    }

    @Test
    void concatMapWaitsBeforeStartingEachNextCall() {
        StepVerifier.withVirtualTime(Composition::sequential)
                .expectSubscription()
                .expectNoEvent(Duration.ofMillis(29))
                .thenAwait(Duration.ofMillis(1))
                .expectNext("item-1")
                .expectNoEvent(Duration.ofMillis(19))
                .thenAwait(Duration.ofMillis(11))
                .expectNext("item-2", "item-3")
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
    }

    @Test
    void flatMapSequentialStartsEarlyAndHoldsResultsToPreserveOrder() {
        StepVerifier.withVirtualTime(Composition::orderedConcurrent)
                .expectSubscription()
                .expectNoEvent(Duration.ofMillis(29))
                .thenAwait(Duration.ofMillis(1))
                .expectNext("item-1", "item-2", "item-3")
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
    }

    @Test
    void switchMapCancelsPendingLookupWhenNewQueryArrives() {
        // TestPublisher позволяет отправить новый ввод ровно в нужный момент.
        TestPublisher<String> queries = TestPublisher.create();
        PublisherProbe<String> pending = PublisherProbe.of(Mono.never());
        Flux<String> suggestions = queries.flux()
                .switchMap(query -> query.equals("ко") ? pending.mono() : Mono.just("кофе"));

        StepVerifier.create(suggestions)
                .then(() -> queries.next("ко"))
                .then(pending::assertWasSubscribed)
                .then(() -> queries.emit("коф"))
                .expectNext("кофе")
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
        pending.assertWasCancelled();
    }

    @Test
    void takeCompletesDownstreamAndCancelsUpstream() {
        PublisherProbe<Integer> upstream = PublisherProbe.of(Flux.range(1, 10));
        StepVerifier.create(upstream.flux().take(3), 0)
                .thenRequest(2)
                .expectNext(1, 2)
                .thenRequest(1)
                .expectNext(3)
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
        upstream.assertWasCancelled();
    }

    @Test
    void fastSourceDropsTicksUntilSubscriberHasDemand() {
        List<Long> dropped = new ArrayList<>();
        StepVerifier.withVirtualTime(() -> FlowControl.ticksDroppingWhenBusy(dropped::add), 0)
                .expectSubscription()
                .expectNoEvent(Duration.ofSeconds(2))
                .thenRequest(1)
                .thenAwait(Duration.ofSeconds(1))
                .expectNext(2L)
                .thenCancel()
                .verify(VERIFY_TIMEOUT);
        assertEquals(List.of(0L, 1L), dropped);
    }

    @Test
    void subscribeOnMovesSourceAndPublishOnMovesFollowingOperator() {
        Scheduler io = Schedulers.newBoundedElastic(1, 10, "test-io");
        Scheduler processing = Schedulers.newSingle("test-processing");
        List<String> logs = new ArrayList<>();
        try {
            StepVerifier.create(Execution.readLegacy(io, processing, logs::add))
                    .expectNext("товар")
                    .expectComplete()
                    .verify(VERIFY_TIMEOUT);
            assertTrue(logs.get(0).contains("на test-io-"));
            assertTrue(logs.get(1).contains("на test-processing-"));
        } finally {
            processing.dispose();
            io.dispose();
        }
    }

    @Test
    void eachSubscriberHasItsOwnContextAcrossThreadSwitch() {
        Mono<String> response = Execution.traced(Schedulers.parallel());
        StepVerifier.create(Flux.concat(
                        response.contextWrite(context -> context.put(Execution.TRACE_ID, "A")),
                        response.contextWrite(context -> context.put(Execution.TRACE_ID, "B")),
                        response))
                .expectNext("A: готово", "B: готово", "без traceId: готово")
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
    }

    @Test
    void replaySinkServesLastTwoEventsAfterCompletion() {
        Sinks.Many<String> updates = Sharing.recentUpdates();
        Sharing.sendUpdate(updates, "a");
        Sharing.sendUpdate(updates, "b");
        Sharing.sendUpdate(updates, "c");
        assertEquals(Sinks.EmitResult.OK, updates.tryEmitComplete());

        StepVerifier.create(updates.asFlux())
                .expectNext("b", "c")
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
    }

    @Test
    void cachedMonoSharesTheFirstLoad() {
        AtomicInteger loads = new AtomicInteger();
        Mono<Integer> cached = Sources.deferredValue(loads::incrementAndGet).cache();
        StepVerifier.create(Flux.concat(cached, cached))
                .expectNext(1, 1)
                .expectComplete()
                .verify(VERIFY_TIMEOUT);
        assertEquals(1, loads.get());
    }

    @Test
    void liveSinkCanDeliverToFastSubscriberWhileSlowOneHasNoDemand() {
        Sinks.Many<String> updates = Sharing.liveUpdates();
        List<String> fast = new ArrayList<>();
        var subscription = updates.asFlux().subscribe(fast::add);
        try {
            StepVerifier.create(updates.asFlux(), 0)
                    .then(() -> Sharing.sendUpdate(updates, "только быстрому"))
                    .thenRequest(1)
                    .then(() -> Sharing.sendUpdate(updates, "обоим"))
                    .expectNext("обоим")
                    .thenCancel()
                    .verify(VERIFY_TIMEOUT);
            assertEquals(List.of("только быстрому", "обоим"), fast);
        } finally {
            subscription.dispose();
        }
    }
}
