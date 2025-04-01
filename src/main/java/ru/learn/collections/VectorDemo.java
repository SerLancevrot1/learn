package ru.learn.collections;

import java.util.Vector;

public class VectorDemo {

    /*
    Класс Vector реализует динамический массив. Он похож на ArrayList, но с двумя отличиями:

    Vector синхронизирован.
    Vector содержит много устаревших методов, которые не являются частью структуры коллекций.
     */

    public static void main(String[] args) {
        Vector<Integer> vector = new Vector<>();
        vector.add(0);
        vector.add(1);
        vector.add(2);
        vector.add(3);
        vector.add(4);
        vector.add(5);
        vector.add(6);
        vector.add(7);
        vector.add(8);
        vector.add(9);

        vector.forEach(System.out::println);
    }
}
