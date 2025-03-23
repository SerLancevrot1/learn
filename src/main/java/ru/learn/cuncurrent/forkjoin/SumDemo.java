package ru.learn.cuncurrent.forkjoin;

import java.util.concurrent.ForkJoinPool;
import org.springframework.util.StopWatch;

public class SumDemo {

    public static void main(String[] args) {
        int arrSize = 1_000_000_000;
        int[] arr = new int[arrSize];

        fillArr(arr);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();


        ForkJoinPool forkJoinPool = new ForkJoinPool();
        long calcSum = forkJoinPool.invoke(new SumTask(arr, 0, arr.length));

        System.out.println(calcSum);
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeSeconds());

        System.out.println("Gauss sum: " + gaussSum(arrSize));
    }

    private static long gaussSum(int n) {
        return (long) n * (n + 1) / 2;
    }

    private static void fillArr(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = i + 1;
        }
    }
}
