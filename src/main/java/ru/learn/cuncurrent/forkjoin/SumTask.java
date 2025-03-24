package ru.learn.cuncurrent.forkjoin;

import java.util.concurrent.RecursiveTask;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SumTask extends RecursiveTask<Long> {

    private static final Integer THRESHOLD = 1000;

    private final int[] arr;

    private final int start;

    private final int end;

    @Override
    protected Long compute() {
        if (end - start <= THRESHOLD) {
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += arr[i];
            }
            return sum;
        } else {
            int mid = (start + end) / 2;
            SumTask leftTask = new SumTask(arr, start, mid);
            SumTask rightTask = new SumTask(arr, mid, end);
            leftTask.fork();
            long rightResult = rightTask.compute();
            long leftResult = leftTask.join();
            return leftResult + rightResult;
        }
    }
}
