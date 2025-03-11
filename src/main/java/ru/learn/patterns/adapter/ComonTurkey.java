package ru.learn.patterns.adapter;

public class ComonTurkey implements Turkey {

    @Override
    public void gobble() {
        System.out.println("Gobble");
    }

    @Override
    public void heightJump() {
        System.out.println("Height jump");
    }
}
