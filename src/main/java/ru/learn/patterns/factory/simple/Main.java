package ru.learn.patterns.factory.simple;

import ru.learn.patterns.factory.simple.pizza.Pizza;

public class Main {
    public static void main(String[] args) {
        PizzaStore pizzaStore = new PizzaStore();
        Pizza pizza = pizzaStore.orderPizza("pepperoni");
        System.out.println(pizza.getClass());
    }
}
