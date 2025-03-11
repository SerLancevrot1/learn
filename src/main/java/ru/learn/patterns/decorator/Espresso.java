package ru.learn.patterns.decorator;

public class Espresso implements Coffee {
    @Override
    public double getCost() {
        return 150;
    }

    @Override
    public String getDescription() {
        return "Espresso";
    }
}
