package ru.learn.stream.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamApiCases {

    public static void main(String[] args) {
        countCanSkipPeek();
        streamIsSingleUse();
        takeWhileReadsOnlyPrefix();
        filteringInsideGroupKeepsKey();
        sortedSetUsesComparatorForEquality();
        findFirstCannotWrapNull();
        flatMapClosesInnerStreams();
    }

    private static void countCanSkipPeek() {
        long count = List.of("a", "b", "c").stream()
                .peek(value -> System.out.println("peek: " + value))
                .count();
        System.out.println("1. count: " + count);
    }

    private static void streamIsSingleUse() {
        var words = List.of("java", "sql");
        var stream = words.stream();
        System.out.println("2. first terminal operation: " + stream.count());
        try {
            stream.findFirst();
        } catch (IllegalStateException exception) {
            System.out.println("   second terminal operation: IllegalStateException");
        }
        System.out.println("   new stream: " + words.stream().findFirst().orElseThrow());
    }

    private static void takeWhileReadsOnlyPrefix() {
        var values = List.of(2, 4, 1, 6);
        var prefix = values.stream().takeWhile(n -> n % 2 == 0).toList();
        var allMatches = values.stream().filter(n -> n % 2 == 0).toList();
        System.out.println("3. takeWhile: " + prefix + ", filter: " + allMatches);
    }

    private static void filteringInsideGroupKeepsKey() {
        record Order(String shop, boolean paid) {}

        var orders = List.of(new Order("A", true), new Order("B", false));
        var counts = orders.stream()
                .collect(Collectors.groupingBy(Order::shop,
                        Collectors.filtering(Order::paid, Collectors.counting())));
        System.out.println("4. paid orders in shop B: " + counts.get("B"));
    }

    private static void sortedSetUsesComparatorForEquality() {
        var words = List.of("cat", "dog", "bear");
        var byLength = Comparator.comparingInt(String::length);
        var distinctWords = words.stream().sorted(byLength).distinct().toList();
        var distinctLengths = words.stream()
                .collect(Collectors.toCollection(() -> new TreeSet<>(byLength)));
        System.out.println("5. distinct: " + distinctWords + ", TreeSet: " + distinctLengths);
    }

    private static void findFirstCannotWrapNull() {
        var values = Stream.of(null, "ready").toList();
        try {
            values.stream().findFirst();
        } catch (NullPointerException exception) {
            System.out.println("6. findFirst: NullPointerException");
        }
        var firstValue = values.stream().flatMap(Stream::ofNullable).findFirst();
        System.out.println("   first non-null value: " + firstValue.orElseThrow());
    }

    private static void flatMapClosesInnerStreams() {
        var closed = new ArrayList<String>();
        var values = Stream.of("left", "right")
                .flatMap(name -> Stream.of(name + "-1", name + "-2")
                        .onClose(() -> closed.add(name)))
                .toList();
        System.out.println("7. flatMap: " + values + ", closed: " + closed);
    }
}
