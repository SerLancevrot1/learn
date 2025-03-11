package ru.learn.patterns.strategy;

public class QuackSilent implements QuackBehaviour {
    @Override
    public void quack() {
        System.out.println("quack silent");
    }
}
