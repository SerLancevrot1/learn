package ru.learn.generic;

public class CommonClassConstraint {

    public <T extends Number & Comparable<T>> void printWithConstraint(T t) {
        System.out.println(t);
    }

}
