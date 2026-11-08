package ru.learn.patterns.strategy;

/**
 * Strategy — позволяет комбинировать варианты полёта и голоса утки.
 * Duck делегирует каждое действие отдельной стратегии, которую выбирает конкретный вид утки.
 */
public class Main {
    public static void main(String[] args) {
        var fatDuck = new FatDuck();
        var slimDuck = new SlimDuck();

        fatDuck.showYourself();

        slimDuck.showYourself();
    }
}
