package ru.learn.leet.code;

import java.util.HashSet;
import java.util.Set;

import ru.learn.leet.code.util.ListNode;

public class LinkedListCycle {

    public static boolean hasCycle(ListNode head) {
        if (head == null) {
            return false;
        }

        Set<ListNode> set = new HashSet<>();

        ListNode curr = head;
        while (curr.next != null) {
            if (set.contains(curr)) {
                return true;
            } else {
                set.add(curr);
                curr = curr.next;
            }
        }
        return false;
    }

    public static void main(String[] args) {

        System.out.println(hasCycle(
                new ListNode(1,
                             new ListNode(2,
                                          new ListNode(3,
                                                       new ListNode(4,
                                                                    new ListNode(5)))))));

        System.out.println(hasCycle(
                new ListNode(1,
                             new ListNode(2,
                                          new ListNode(3,
                                                       new ListNode(4,
                                                                    new ListNode(5,
                                                                                 new ListNode(6))))))));

        System.out.println(hasCycle(
                new ListNode(1,
                             new ListNode(2,
                                          new ListNode(3,
                                                       new ListNode(4,
                                                                    new ListNode(5,
                                                                                 new ListNode(6,
                                                                                              new ListNode(7)))))))));

//        var listNode = new ListNode(1,
//                                    new ListNode(2,
//                                                 new ListNode(3,
//                                                              new ListNode(4))));
//        System.out.println(hasCycle(new ListNode(listNode, listNode)));

    }



}
