package ru.learn.patterns.strategy;

public class Main {
    public static void main(String[] args) {
        var fatDuck = new FatDuck();
        var slimDuck = new SlimDuck();

        fatDuck.showYourself();

        slimDuck.showYourself();
    }
}
