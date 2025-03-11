package ru.learn.patterns.adapter;

public class CommonDuck implements Duck {

    @Override
    public void quack() {
        System.out.println("Quack");
    }

    @Override
    public void fly() {
        System.out.println("fly like duck");
    }
}
