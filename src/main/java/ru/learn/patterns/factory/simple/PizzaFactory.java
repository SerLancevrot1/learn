package ru.learn.patterns.factory.simple;

import ru.learn.patterns.factory.simple.pizza.Margaritta;
import ru.learn.patterns.factory.simple.pizza.Pepperoni;
import ru.learn.patterns.factory.simple.pizza.Pizza;

public class PizzaFactory {

    public static Pizza getPizzaByCode(String code) {
        return switch (code) {
            case "pepperoni" -> new Pepperoni();
            case "margaritta" -> new Margaritta();
            default -> throw new RuntimeException();
        };
    }
}
