package ru.learn.patterns.iterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator — даёт единый способ обхода диапазона через for-each.
 * Диапазон хранит границы, а каждый итератор ведёт собственный курсор.
 */
public class Main {
    public static void main(String[] args) {
        for (int number : new Range(2, 5)) {
            System.out.println(number);
        }
    }

    record Range(int start, int endExclusive) implements Iterable<Integer> {
        @Override
        public Iterator<Integer> iterator() {
            // У каждого обхода собственный курсор, поэтому два итератора не влияют друг на друга.
            return new Iterator<>() {
                private int current = start;

                @Override
                public boolean hasNext() {
                    return current < endExclusive;
                }

                @Override
                public Integer next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return current++;
                }
            };
        }
    }
}
