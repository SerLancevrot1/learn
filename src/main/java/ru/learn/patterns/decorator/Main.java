package ru.learn.patterns.decorator;

public class Main {
    public static void main(String[] args) {
        Coffee raf = new Raf();

        raf = new Caramel(raf);
        raf = new Caramel(raf);
        raf = new SoyNutMilk(raf);

        System.out.println(raf.getDescription());
        System.out.println(raf.getCost());

        Coffee latte = new Latte();

        latte = new SoyNutMilk(latte);
        latte = new SoyNutMilk(latte);

        System.out.println(latte.getDescription());
        System.out.println(latte.getCost());
    }
}
