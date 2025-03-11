package ru.learn.patterns.factory.method;

import ru.learn.patterns.factory.method.pizza.LondonPeperoniPizza;
import ru.learn.patterns.factory.method.pizza.MoscowPeperoniPizza;
import ru.learn.patterns.factory.method.pizza.Pizza;
import ru.learn.patterns.factory.method.store.LondonPizzaStore;
import ru.learn.patterns.factory.method.store.MoscowPizzaStore;

public class Main {
    public static void main(String[] args) {
        MoscowPizzaStore moscowPizzaStore = new MoscowPizzaStore();
        LondonPizzaStore londonPizzaStore = new LondonPizzaStore();

        Pizza moscowPeperoniPizza = moscowPizzaStore.orderPizza("pepperoni");
        Pizza londonPeperoniPizza = londonPizzaStore.orderPizza("pepperoni");

        System.out.println(moscowPeperoniPizza instanceof MoscowPeperoniPizza);
        System.out.println(londonPeperoniPizza instanceof LondonPeperoniPizza);
    }
}
