package ru.learn.leet.code.easy;

import ru.learn.leet.code.util.ListNode;

public class MiddleOfTheLinkedList {
    public static ListNode middleNode(ListNode head) {

        ListNode current = head;
        int count = 1;
        while (current.next != null) {
            current = current.next;
            count++;
        }
        int resIndex = count / 2 + 1;

        current = head;
        count = 1;
        while (count != resIndex) {
            current = current.next;
            count++;
        }
        return current;

    }

    public static void main(String[] args) {

        System.out.println(middleNode(
                new ListNode(1,
                             new ListNode(2,
                                          new ListNode(3,
                                                       new ListNode(4,
                                                                    new ListNode(5)))))).toString());

        System.out.println(middleNode(
                new ListNode(1,
                             new ListNode(2,
                                          new ListNode(3,
                                                       new ListNode(4,
                                                                    new ListNode(5,
                                                                                 new ListNode(6))))))).toString());

        System.out.println(middleNode(
                new ListNode(1,
                             new ListNode(2,
                                          new ListNode(3,
                                                       new ListNode(4,
                                                                    new ListNode(5,
                                                                                 new ListNode(6,
                                                                                              new ListNode(7)))))))).toString());


    }



}
