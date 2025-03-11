package ru.learn.leet.code;

import ru.learn.leet.code.util.ListNode;

public class MergeTwoSortedLists {
    public static ListNode mergeTwoLists(ListNode list1, ListNode list2) {
        ListNode res = list1;

        if (list1 == null) {
            return list2;
        }

        while (list1 != null && list2 != null) {
            if (list1.val > list2.val) {
                int min = list2.val;
                int max = list1.val;
                ListNode next = list1.next;
                list1.val = min;
                list1.next = new ListNode(max, next);
            } else {
                ListNode next = list1.next;
                int temp = list2.val;
                list1.next = new ListNode(temp, next);
            }
            list1 = list1.next.next;
            list2 = list2.next;
        }
        return res;
    }

    public static void main(String[] args) {
        System.out.println(mergeTwoLists(new ListNode(5),
                                         new ListNode(1, new ListNode(2, new ListNode(3)))));
        System.out.println(mergeTwoLists(new ListNode(1, new ListNode(2, new ListNode(3))),
                                         new ListNode(2, new ListNode(3, new ListNode(4)))));
    }


}
