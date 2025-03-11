package ru.learn.patterns.factory.abstractt;

public class Main {

    public static void main(String[] args) {
        var phoneFabric = new PhoneFabric();

        System.out.println(phoneFabric.buildPhone("samsung"));
        System.out.println(phoneFabric.buildPhone("apple"));
    }
}
