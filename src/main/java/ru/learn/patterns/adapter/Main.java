package ru.learn.patterns.adapter;

public class Main {

    public static void main(String[] args) {

        var comonTurkey = new ComonTurkey();
        comonTurkey.gobble();
        comonTurkey.heightJump();

        checkDuck(new CommonDuck());

        //индейка влезла в интерфейс утки
        checkDuck(new DuckAdapter(comonTurkey));
    }

    private static void checkDuck(Duck duck) {
        duck.quack();
        duck.fly();
    }
}
