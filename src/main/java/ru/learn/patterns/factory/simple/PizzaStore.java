package ru.learn.patterns.factory.simple;

import ru.learn.patterns.factory.simple.pizza.Pizza;

public class PizzaStore {

    public Pizza orderPizza(String code) {
        var pizza = PizzaFactory.getPizzaByCode(code);
        pizza.cut();
        pizza.box();
        return pizza;
    }
}
