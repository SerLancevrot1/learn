package ru.learn.patterns.decorator;

public class Raf implements Coffee {
    @Override
    public double getCost() {
        return 299;
    }

    @Override
    public String getDescription() {
        return "Raf";
    }
}
