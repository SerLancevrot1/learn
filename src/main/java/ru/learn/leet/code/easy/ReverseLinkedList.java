package ru.learn.leet.code.easy;

import ru.learn.leet.code.util.ListNode;

public class ReverseLinkedList {

    public static ListNode reverseList(ListNode head) {
        if (head == null) {
            return null;
        }

        ListNode cur = new ListNode(head.val);

        while (head.next != null) {
            cur = new ListNode(head.next.val, cur);
            head = head.next;
        }
        return cur;
    }

    public static void main(String[] args) {
        System.out.println(reverseList(
                new ListNode(1,
                             new ListNode(2,
                                          new ListNode(2,
                                                       new ListNode(3,
                                                                    new ListNode(3,
                                                                                 new ListNode(4,
                                                                                              new ListNode(5)))))))).toString());
    }
}
