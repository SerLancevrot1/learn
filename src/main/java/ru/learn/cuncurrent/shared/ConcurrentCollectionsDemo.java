package ru.learn.cuncurrent.shared;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Показывает атомарное обновление по ключу в ConcurrentHashMap и обход снимка
 * в CopyOnWriteArrayList. Эти коллекции решают разные задачи.
 */
public class ConcurrentCollectionsDemo {

    public static void main(String[] args) throws InterruptedException {
        countVisits();
        notifySubscribers();
    }

    private static void countVisits() throws InterruptedException {
        ConcurrentMap<String, Integer> visits = new ConcurrentHashMap<>();
        // merge() объединяет чтение и запись по ключу в одну атомарную операцию.
        Runnable visitHome = () -> visits.merge("/", 1, Integer::sum);
        Thread first = new Thread(visitHome, "visitor-1");
        Thread second = new Thread(visitHome, "visitor-2");
        first.start();
        second.start();
        first.join();
        second.join();
        System.out.println("Посещений главной: " + visits.get("/"));
    }

    private static void notifySubscribers() throws InterruptedException {
        List<String> subscribers = new CopyOnWriteArrayList<>(Arrays.asList("Анна", "Борис"));
        Iterator<String> currentDispatch = subscribers.iterator();
        Thread subscribe = new Thread(() -> subscribers.add("Вера"), "subscriber");
        subscribe.start();
        subscribe.join();

        // Итератор видит список на момент создания. Копирование при записи выгодно при редких изменениях.
        currentDispatch.forEachRemaining(name -> System.out.println("Текущая рассылка: " + name));
        System.out.println("Следующая рассылка: " + subscribers);
    }
}
