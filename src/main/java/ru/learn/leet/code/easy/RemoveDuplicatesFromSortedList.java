package ru.learn.leet.code.easy;

import ru.learn.leet.code.util.ListNode;

public class RemoveDuplicatesFromSortedList {

    public static ListNode deleteDuplicates(ListNode head) {
        ListNode current = head;

        while (current.next != null) {
            if (current.val == current.next.val) {
                current.next = current.next.next;
            } else {
                current = current.next;
            }
        }

        return head;
    }

    public static void main(String[] args) {
        System.out.println(deleteDuplicates(
                new ListNode(1,
                             new ListNode(2,
                                          new ListNode(2,
                                                       new ListNode(3,
                                                                    new ListNode(3,
                                                                                 new ListNode(4,
                                                                                              new ListNode(5)))))))).toString());
    }
}
