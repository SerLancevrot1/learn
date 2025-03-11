package ru.learn.generic;

public class CommonClassWithGenericMethods {

    public <T> void printAny(T t) {
        System.out.println(t.toString());
    }

    public <T> T printAnyAndReturn(T t) {
        System.out.println(t.toString());
        return t;
    }
}
