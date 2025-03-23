package ru.learn.cuncurrent.forkjoin;

import org.springframework.util.StopWatch;

public class SumStandardDemo {

    public static void main(String[] args) {
        int arrSize = 1_000_000_000;
        int[] arr = new int[arrSize];

        fillArr(arr);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        long calcSum = 0;

        for (int i = 0; i < arr.length; i++) {
            calcSum += arr[i];
        }

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
