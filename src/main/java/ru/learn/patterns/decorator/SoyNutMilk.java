package ru.learn.patterns.decorator;

public class SoyNutMilk extends CondimentDecorator {
    public SoyNutMilk(Coffee coffee) {
        this.coffee = coffee;
    }

    @Override
    public double getCost() {
        return coffee.getCost() + 50;
    }

    @Override
    public String getDescription() {
        return coffee.getDescription() + ", soy nut milk";
    }
}
