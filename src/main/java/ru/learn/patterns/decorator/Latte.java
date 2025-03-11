package ru.learn.patterns.decorator;

public class Latte implements Coffee {
    @Override
    public double getCost() {
        return 200;
    }

    @Override
    public String getDescription() {
        return "Latte";
    }
}
