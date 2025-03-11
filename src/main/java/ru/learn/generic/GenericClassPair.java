package ru.learn.generic;

import java.util.Map;

public class GenericClassPair<K,V> {

    public void printAny(K k, V v) {
        System.out.println(k.toString() + " " + v.toString());
    }

    public Map<K, V> printAnyAndReturn(K k, V v) {
        return Map.of(k, v);
    }

}
