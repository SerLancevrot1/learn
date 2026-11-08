package ru.learn.patterns.singleton;

/**
 * Singleton — предоставляет одну общую точку доступа к экземпляру класса.
 * Два вызова getInstance() возвращают одну и ту же ссылку в пределах загрузчика классов.
 */
public class Main {
    public static void main(String[] args) {
        Singleton first = Singleton.getInstance();
        Singleton second = Singleton.getInstance();
        System.out.println("Same instance: " + (first == second));
    }
}
