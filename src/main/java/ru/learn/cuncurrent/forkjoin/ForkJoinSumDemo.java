package ru.learn.cuncurrent.forkjoin;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.IntStream;

/**
 * Складывает выручку по дням последовательно, через Fork/Join и через parallel stream.
 * Все три способа работают с одним массивом и дают одинаковую сумму.
 */
public class ForkJoinSumDemo {

    public static void main(String[] args) {
        int[] sales = IntStream.rangeClosed(1, 10_000).toArray();
        long sequential = Arrays.stream(sales).asLongStream().sum();

        ForkJoinPool pool = new ForkJoinPool(2);
        long forkJoin;
        try {
            forkJoin = pool.invoke(new SumTask(sales, 0, sales.length));
        } finally {
            pool.shutdown();
        }

        // Сумма независимых элементов не требует изменяемого общего счётчика.
        long parallelStream = Arrays.stream(sales).parallel().asLongStream().sum();
        System.out.println("Последовательно: " + sequential);
        System.out.println("Fork/Join: " + forkJoin);
        System.out.println("Parallel stream: " + parallelStream);
    }

    /** Считает сумму по полуоткрытому диапазону [from, to) без копирования массива. */
    private static class SumTask extends RecursiveTask<Long> {
        private static final long serialVersionUID = 1L;
        private static final int THRESHOLD = 1_000;

        private final int[] sales;
        private final int from;
        private final int to;

        private SumTask(int[] sales, int from, int to) {
            this.sales = sales;
            this.from = from;
            this.to = to;
        }

        @Override
        protected Long compute() {
            if (to - from <= THRESHOLD) {
                // Для маленького участка дешевле пройтись циклом.
                long sum = 0;
                for (int index = from; index < to; index++) {
                    sum += sales[index];
                }
                return sum;
            }

            int mid = from + (to - from) / 2;
            SumTask left = new SumTask(sales, from, mid);
            left.fork(); // Левый участок доступен другим работникам пула.
            long rightSum = new SumTask(sales, mid, to).compute();
            return rightSum + left.join();
        }
    }
}
