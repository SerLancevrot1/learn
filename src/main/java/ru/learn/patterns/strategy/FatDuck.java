package ru.learn.patterns.strategy;

public class FatDuck extends Duck {

    public FatDuck() {
        quackBehaviour = new QuackLoud();
        flyBehaviour = new FlyHigh();
    }

    @Override
    public void display() {
        System.out.println("<fat>");
    }
}
