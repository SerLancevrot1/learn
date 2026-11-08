package ru.learn.patterns.singleton;

public class Singleton {

    // volatile обеспечивает безопасную публикацию экземпляра при чтении вне блока synchronized.
    private volatile static Singleton instance;

    private Singleton(){}

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                // Пока поток ждал блокировку, другой поток мог уже создать экземпляр.
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
