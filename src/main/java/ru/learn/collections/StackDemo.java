package ru.learn.collections;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;

public class StackDemo {

    public static void main(String[] args) {
        Deque<Integer> deque = new ArrayDeque<>();
//        deque.peek();

        deque.push(1);
        deque.push(2);
        deque.push(3);
        deque.push(4);

        System.out.println(deque);

        while (!deque.isEmpty()) System.out.println(deque.pop());
    }
}
