package ru.learn.patterns.decorator;

public class Caramel extends CondimentDecorator {
    public Caramel(Coffee coffee) {
        this.coffee = coffee;
    }

    @Override
    public double getCost() {
        return coffee.getCost() + 30;
    }

    @Override
    public String getDescription() {
        return coffee.getDescription() + ", caramel";
    }
}
