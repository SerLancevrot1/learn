package ru.learn.patterns.factory.method.store;

import ru.learn.patterns.factory.method.pizza.LondonMargarittaPizza;
import ru.learn.patterns.factory.method.pizza.LondonPeperoniPizza;
import ru.learn.patterns.factory.method.pizza.Pizza;

public class LondonPizzaStore extends PizzaStore {
    @Override
    protected Pizza createPizza(String type) {
        return switch (type) {
            case "pepperoni" -> new LondonPeperoniPizza();
            case "margaritta" -> new LondonMargarittaPizza();
            default -> throw new RuntimeException();
        };
    }
}
