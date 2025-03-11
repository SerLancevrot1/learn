package ru.learn.patterns.strategy;

public class FlyLow implements FlyBehaviour {
    @Override
    public void fly() {
        System.out.println("fly low");
    }
}
