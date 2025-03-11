package ru.learn.patterns.decorator;

public class SimpleSoyMilk extends CondimentDecorator {
    public SimpleSoyMilk(Coffee coffee) {
        this.coffee = coffee;
    }

    @Override
    public double getCost() {
        return coffee.getCost() + 40;
    }

    @Override
    public String getDescription() {
        return coffee.getDescription() + ", simple soy milk";
    }
}
