package ru.learn.patterns.factory.simple;

import ru.learn.patterns.factory.simple.pizza.Pizza;

/**
 * Simple Factory — собирает правила выбора пиццы по коду в одном месте.
 * Магазин использует готовую Pizza, а выбор конкретного класса делает PizzaFactory.
 */
public class Main {
    public static void main(String[] args) {
        PizzaStore pizzaStore = new PizzaStore();
        Pizza pizza = pizzaStore.orderPizza("pepperoni");
        System.out.println(pizza.getClass());
    }
}
