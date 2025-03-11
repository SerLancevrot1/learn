package ru.learn.patterns.strategy;

public class FlyHigh implements FlyBehaviour {
    @Override
    public void fly() {
        System.out.println("fly high");
    }
}
