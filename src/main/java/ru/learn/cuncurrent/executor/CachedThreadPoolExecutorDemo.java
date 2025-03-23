package ru.learn.cuncurrent.executor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.util.StopWatch;

public class CachedThreadPoolExecutorDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        StopWatch s1 = new StopWatch();
        s1.start();
        ExecutorService es = Executors.newCachedThreadPool();
        Future<?> submit = null;
        for (int i = 0; i < 10000; i++) {
            submit = es.submit(new GenerateRandomIntegerTask());
        }
        //debug -> es -> worker -> count
        submit.get();
        s1.stop();
        System.out.println(s1.getTotalTimeSeconds());

    }
}
