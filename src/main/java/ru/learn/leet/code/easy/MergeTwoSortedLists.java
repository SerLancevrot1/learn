package ru.learn.leet.code.easy;

import ru.learn.leet.code.util.ListNode;

public class MergeTwoSortedLists {
    public static ListNode mergeTwoLists(ListNode list1, ListNode list2) {
        ListNode head = new ListNode();
        ListNode res = head;
        while (list1 != null && list2 != null) {
            if (list1.val >= list2.val) {
                res.next = list2;
                list2 = list2.next;
            } else {
                res.next = list1;
                list1 = list1.next;
            }
            System.out.println(res.val);
            res = res.next;
        }
        if (list1 != null) {
            res.next = list1;
        }else {
            res.next = list2;
        }
        return head.next;

    }



    public static void main(String[] args) {
        System.out.println(mergeTwoLists(new ListNode(5),
                                         new ListNode(1, new ListNode(2, new ListNode(3)))));

        System.out.println(mergeTwoLists(new ListNode(1, new ListNode(2, new ListNode(3))),
                                         new ListNode(2, new ListNode(3, new ListNode(4)))));
    }


}
