package ru.learn.collections;

import java.util.LinkedHashMap;

public class LinkedHashMapDemo {
    /*
    Данная структура может слегка уступать по производительности родительскому HashMap, при этом время выполнения операций add(), contains(),
     remove() остается константой — O(1). Понадобится чуть больше места в памяти для хранения элементов и их связей, но это совсем небольшая
      плата за дополнительные фишечки.

    Вообще, из-за того что всю основную работу на себя берет родительский класс, серьезных отличий в реализации HashMap и LinkedHashMap
     не много. Можно упомянуть о парочке мелких:

    Методы transfer() и containsValue() устроены чуть проще из-за наличия двунаправленной связи между элементами;
    В классе LinkedHashMap.Entry реализованы методы recordRemoval() и recordAccess() (тот самый, который помещает элемент в конец
     при accessOrder = true). В HashMap оба этих метода пустые.

     */

    public static void main(String[] args) {
        LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "1");
        map.put(6, "6");
        map.put(2, "2");
        map.put(3, "3");
        map.put(5, "5");
        map.put(4, "4");

        map.forEach((k, v) -> System.out.println(k + " " + v));
    }
}
