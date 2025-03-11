package ru.learn.generic;

public class GenericClass<T> {

    public void printAny(T t) {
        System.out.println(t.toString());
    }

    public T printAnyAndReturn(T t) {
        System.out.println(t.toString());
        return t;
    }

    public String returnString(T t) {
        return t.toString();
    }
}
