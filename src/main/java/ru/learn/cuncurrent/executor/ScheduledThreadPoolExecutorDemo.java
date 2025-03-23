package ru.learn.cuncurrent.executor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.util.StopWatch;

public class ScheduledThreadPoolExecutorDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("cores = " + cores);
        ScheduledExecutorService es = Executors.newScheduledThreadPool(cores - 1);

        es.schedule(new GenerateRandomIntegerTaskWithId(1), 10, TimeUnit.SECONDS);
        es.schedule(new GenerateRandomIntegerTaskWithId(2), 8, TimeUnit.SECONDS);
        es.schedule(new GenerateRandomIntegerTaskWithId(3), 5, TimeUnit.SECONDS);
        es.schedule(new GenerateRandomIntegerTaskWithId(4), 3, TimeUnit.SECONDS);
        es.schedule(new GenerateRandomIntegerTaskWithId(5), 1, TimeUnit.SECONDS);
        es.schedule(new GenerateRandomIntegerTaskWithId(6), 0, TimeUnit.SECONDS);

    }
}
