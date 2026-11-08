package ru.learn.patterns.factory.method.store;

import ru.learn.patterns.factory.method.pizza.Pizza;

public abstract class PizzaStore {

    public Pizza orderPizza(String type) {
        Pizza pizza = createPizza(type);
        pizza.cook();
        pizza.cut();
        pizza.box();
        return pizza;
    }

    // Подкласс выбирает продукт, а orderPizza() задаёт общую последовательность действий.
    protected abstract Pizza createPizza(String type);
}
