package ru.learn.cuncurrent.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.util.StopWatch;

public class FixedThreadPoolCallableExecutorDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("cores = " + cores);
        List<Future<Integer>> futures = new ArrayList<>();

        StopWatch s1 = new StopWatch();
        s1.start();
        ExecutorService es = Executors.newFixedThreadPool(cores - 1);
        for (int i = 0; i < 100; i++) {
            futures.add(es.submit(new GenerateRandomIntegerCallableTask()));
        }

        futures.forEach(f -> {
            try {
                System.out.println(f.get());
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        s1.stop();
        System.out.println(s1.getTotalTimeSeconds());

    }
}
