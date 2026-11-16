package ru.learn.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Атомарное обновление, уникальность, навигация и разные гарантии обхода. */
public class ConcurrentCollectionsDemo {
    public static void main(String[] args) throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            countAndDeduplicate(workers);
            snapshotDispatch(workers);
        } finally {
            workers.shutdownNow();
        }
        sortedCollections();
        synchronizedList();
    }

    private static void countAndDeduplicate(ExecutorService workers) throws Exception {
        ConcurrentMap<String, Integer> attempts = new ConcurrentHashMap<>();
        Set<String> seenEvents = ConcurrentHashMap.newKeySet();
        Future<Boolean> first = workers.submit(() -> registerDelivery(attempts, seenEvents));
        Future<Boolean> second = workers.submit(() -> registerDelivery(attempts, seenEvents));
        boolean firstSeen = first.get(5, TimeUnit.SECONDS);
        boolean secondSeen = second.get(5, TimeUnit.SECONDS);

        System.out.println("Попыток доставки: " + attempts.get("order-created-42"));
        System.out.println("Впервые принят одним потоком: " + (firstSeen ^ secondSeen));
    }

    private static boolean registerDelivery(ConcurrentMap<String, Integer> attempts, Set<String> seenEvents) {
        // merge атомарен для одного ключа. add атомарно сообщает, был ли ID новым.
        // Каждая операция защищает свою коллекцию и может наблюдаться отдельно.
        String eventId = "order-created-42";
        attempts.merge(eventId, 1, Integer::sum);
        return seenEvents.add(eventId);
    }

    private static void snapshotDispatch(ExecutorService workers) throws Exception {
        CopyOnWriteArrayList<String> subscribers = new CopyOnWriteArrayList<>(List.of("Анна", "Борис"));
        Iterator<String> dispatch = subscribers.iterator();
        workers.submit(() -> subscribers.addIfAbsent("Вера")).get(5, TimeUnit.SECONDS);

        // Итератор считывает массив на момент создания. Следующий обход получит новый массив.
        dispatch.forEachRemaining(name -> System.out.println("Текущая рассылка: " + name));
        System.out.println("Следующая рассылка: " + subscribers);

        Set<String> channels = new CopyOnWriteArraySet<>(List.of("email", "push"));
        System.out.println("Повторная подписка на email добавлена: " + channels.add("email"));
    }

    private static void sortedCollections() {
        ConcurrentNavigableMap<Integer, String> tariffsByYear = new ConcurrentSkipListMap<>();
        tariffsByYear.put(2026, "Тариф C");
        tariffsByYear.put(2022, "Тариф A");
        tariffsByYear.put(2024, "Тариф B");
        System.out.println("Тариф на 2025 год: " + tariffsByYear.floorEntry(2025).getValue());
        System.out.println("Изменения за 2023–2026: " + tariffsByYear.subMap(2023, true, 2026, true));

        NavigableSet<Integer> reportYears = new ConcurrentSkipListSet<>(List.of(2026, 2022, 2024, 2024));
        System.out.println("Первый отчёт начиная с 2023: " + reportYears.ceiling(2023));
    }

    private static void synchronizedList() {
        List<String> recipients = Collections.synchronizedList(new ArrayList<>());
        // Тот же монитор защищает составную операцию и полный обход.
        synchronized (recipients) {
            if (!recipients.contains("ops@example.test")) {
                recipients.add("ops@example.test");
            }
            for (String recipient : recipients) {
                System.out.println("Адрес уведомлений: " + recipient);
            }
        }
    }
}
