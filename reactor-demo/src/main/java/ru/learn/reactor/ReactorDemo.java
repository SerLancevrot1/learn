package ru.learn.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class ReactorDemo {
    private static final Duration CLI_TIMEOUT = Duration.ofSeconds(5);

    private ReactorDemo() {
    }

    public static void main(String[] args) {
        try {
            runLessons(args);
        } finally {
            // Консольная программа закончила работу. Освобождаем общие Scheduler Reactor,
            // которые использовали таймеры и boundedElastic, чтобы Maven мог сразу выйти.
            Schedulers.shutdownNow();
        }
    }

    private static void runLessons(String[] args) {
        List<Lesson> lessons = List.of(
                new Lesson("sources", ReactorDemo::sources),
                new Lesson("composition", ReactorDemo::composition),
                new Lesson("demand", () -> FlowControl.takeInBatches(System.out::println)),
                new Lesson("threads", ReactorDemo::threads),
                new Lesson("sharing", ReactorDemo::sharing),
                new Lesson("errors", ReactorDemo::errors),
                new Lesson("orders", ReactorDemo::orders));

        if (args.length == 0) {
            lessons.forEach(Lesson::run);
            return;
        }

        if (args.length == 1) {
            for (Lesson lesson : lessons) {
                if (lesson.name().equals(args[0])) {
                    lesson.run();
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Укажите одно имя примера: "
                + lessons.stream().map(Lesson::name).toList());
    }

    private static void sources() {
        AtomicInteger reads = new AtomicInteger();
        Mono<Integer> eager = Sources.assemblyValue(reads::incrementAndGet);
        Mono<Integer> lazy = Sources.deferredValue(reads::incrementAndGet);
        System.out.println("Чтений после сборки: " + reads.get());
        print("just, подписка 1", eager);
        print("just, подписка 2", eager);
        print("fromSupplier, подписка 1", lazy);
        print("fromSupplier, подписка 2", lazy);
        print("generate", Sources.countdown(3).collectList());
    }

    private static void composition() {
        print("map и filter", Composition.labels(Flux.range(1, 5)).collectList());
        print("zip", Composition.summary(Mono.just("кофе"), Mono.just(5)));
        print("then", Composition.afterSave(Mono.empty(), Mono.just("сохранено")));
        // Порядок flatMap зависит от готовности. Тесты используют виртуальное время.
        print("flatMap", Composition.interleaved().collectList());
        print("concatMap", Composition.sequential().collectList());
        print("flatMapSequential", Composition.orderedConcurrent().collectList());
    }

    private static void threads() {
        Scheduler processing = Schedulers.newSingle("demo-processing");
        try {
            // boundedElastic — общий Scheduler Reactor. У своего Scheduler закрываем ресурсы.
            print("Результат", Execution.readLegacy(Schedulers.boundedElastic(), processing, System.out::println));
            print("Context", Execution.traced(processing)
                    .contextWrite(context -> context.put(Execution.TRACE_ID, "request-42")));
        } finally {
            processing.dispose();
        }
    }

    private static void sharing() {
        AtomicInteger reads = new AtomicInteger();
        Mono<Integer> cached = Sources.deferredValue(reads::incrementAndGet).cache();
        // Обе подписки увидят результат одной загрузки. cache() также сохраняет empty и error.
        print("Кеш, подписка 1", cached);
        print("Кеш, подписка 2", cached);
        Sinks.Many<String> updates = Sharing.recentUpdates();
        Sharing.sendUpdate(updates, "создан");
        Sharing.sendUpdate(updates, "оплачен");
        Sharing.sendUpdate(updates, "отправлен");
        Sinks.EmitResult completion = updates.tryEmitComplete();
        if (completion.isFailure()) {
            throw new IllegalStateException("Поток обновлений не завершился: " + completion);
        }
        print("Поздний подписчик", updates.asFlux().collectList());
    }

    private static void errors() {
        print("Оставшиеся значения", Failures.keepingHealthyItems(
                Flux.just("1", "битые данные", "3"),
                value -> System.out.println("Пропускаем: " + value)).collectList());
        AtomicInteger attempts = new AtomicInteger();
        Mono<String> remote = Mono.defer(() -> {
            int attempt = attempts.incrementAndGet();
            System.out.println("Попытка " + attempt);
            return attempt == 1 ? Mono.error(new Failures.TemporaryFailure("Сервис занят")) : Mono.just("ответ");
        });
        print("Retry", Failures.remoteRead(remote, Duration.ofMillis(500), Duration.ofSeconds(2)));
        print("usingWhen", Failures.usingSession(System.out::println, session -> Mono.just("данные сессии")));
    }

    private static void orders() {
        OrderQuotes service = new OrderQuotes(id -> Mono.delay(Duration.ofMillis(30))
                .thenReturn(id * 1_000L));
        print("Расчёт заказа", service.calculate(List.of(3, 1, 2), Mono.just(500L))
                .contextWrite(context -> context.put(Execution.TRACE_ID, "order-request-1")));
    }

    private static void print(String label, Mono<?> result) {
        // Это граница консольной программы. Главный поток ждёт результат максимум 5 секунд.
        // В WebFlux контроллер вернул бы сам Mono, а подпиской управлял бы сервер.
        System.out.println(label + ": " + result.block(CLI_TIMEOUT));
    }

    private record Lesson(String name, Runnable example) {
        void run() {
            System.out.println("\n=== " + name + " ===");
            example.run();
        }
    }
}
