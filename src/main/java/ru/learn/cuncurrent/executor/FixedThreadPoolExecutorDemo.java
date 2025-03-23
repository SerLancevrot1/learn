package ru.learn.cuncurrent.executor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.util.StopWatch;

public class FixedThreadPoolExecutorDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("cores = " + cores);
        StopWatch s1 = new StopWatch();
        s1.start();
        ExecutorService es = Executors.newFixedThreadPool(cores - 1);
        Future<?> submit = null;
        for (int i = 0; i < 100; i++) {
            submit = es.submit(new GenerateRandomIntegerTask());
        }

        submit.get();
        s1.stop();
        System.out.println(s1.getTotalTimeSeconds());

    }
}
