package ru.learn.patterns.strategy;

public class SlimDuck extends Duck {

    public SlimDuck() {
        quackBehaviour = new QuackSilent();
        flyBehaviour = new FlyLow();
    }

    @Override
    public void display() {
        System.out.println("<slim>");
    }
}
