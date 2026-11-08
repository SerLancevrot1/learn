package ru.learn.patterns.decorator;

// Общий интерфейс Coffee позволяет оборачивать и сам напиток, и другую обёртку.
public abstract class CondimentDecorator implements Coffee {

    Coffee coffee;
    //или добавить методы без имплемента интерфейса
}
