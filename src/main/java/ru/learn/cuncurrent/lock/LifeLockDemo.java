package ru.learn.cuncurrent.lock;

public class LifeLockDemo {

    public static void main(String[] args) {
        Eater husband = new Eater("Maxim");
        Eater wife = new Eater("Kristina");

        Spoon s = new Spoon(husband);

        new Thread(() -> husband.eatWith(s, wife)).start();
        new Thread(() -> wife.eatWith(s, husband)).start();
    }
}
