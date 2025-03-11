package ru.learn.patterns.strategy;

public class QuackLoud implements QuackBehaviour {
    @Override
    public void quack() {
        System.out.println("quack loud");
    }
}
