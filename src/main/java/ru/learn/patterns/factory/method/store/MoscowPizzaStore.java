package ru.learn.patterns.factory.method.store;

import ru.learn.patterns.factory.method.pizza.MoscowMargarittaPizza;
import ru.learn.patterns.factory.method.pizza.MoscowPeperoniPizza;
import ru.learn.patterns.factory.method.pizza.Pizza;

public class MoscowPizzaStore extends PizzaStore {
    @Override
    protected Pizza createPizza(String type) {
        return switch (type) {
            case "pepperoni" -> new MoscowPeperoniPizza();
            case "margaritta" -> new MoscowMargarittaPizza();
            default -> throw new RuntimeException();
        };
    }
}
