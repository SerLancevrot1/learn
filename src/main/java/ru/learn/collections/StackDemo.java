package ru.learn.collections;

import java.util.Stack;

public class StackDemo {

    public static void main(String[] args) {
        Stack<Integer> deque = new Stack<>();
        deque.push(1);
        deque.push(2);
        deque.push(3);
        deque.push(4);

        while (!deque.isEmpty()) System.out.println(deque.pop());
    }
}
