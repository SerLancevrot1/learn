package ru.learn.patterns.adapter;

/**
 * Adapter — подключает существующую индейку к коду, который работает с утками.
 * DuckAdapter реализует Duck и переводит его вызовы в операции Turkey.
 */
public class Main {

    public static void main(String[] args) {

        var comonTurkey = new ComonTurkey();
        comonTurkey.gobble();
        comonTurkey.heightJump();

        checkDuck(new CommonDuck());

        // Тот же клиентский метод принимает адаптер, потому что тот реализует Duck.
        checkDuck(new DuckAdapter(comonTurkey));
    }

    private static void checkDuck(Duck duck) {
        duck.quack();
        duck.fly();
    }
}
