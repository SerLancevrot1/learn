package ru.learn.patterns.strategy;

public abstract class Duck {

    FlyBehaviour flyBehaviour;
    QuackBehaviour quackBehaviour;

    // Утка делегирует полёт стратегии, выбранной конкретным подклассом.
    public void fly(){
        flyBehaviour.fly();
    }

    public void quack() {
        quackBehaviour.quack();
    }

    public abstract void display();

    public void showYourself() {
        fly();
        quack();
        display();
    }
}
