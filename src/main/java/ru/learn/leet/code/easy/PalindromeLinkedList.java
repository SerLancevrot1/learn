package ru.learn.leet.code.easy;

import ru.learn.leet.code.util.ListNode;

public class PalindromeLinkedList {

    public static boolean isPalindrome(ListNode head) {
        if (head == null) {
            return true;
        }
        ListNode reverse = null;
        ListNode cur = head;

        while (cur != null) {
            reverse = new ListNode(cur.val, reverse);
            cur = cur.next;
        }

        while (head != null) {
            if (head.val != reverse.val) {
                return false;
            }
            head = head.next;
            reverse = reverse.next;
        }

        return true;
    }

    public static void main(String[] args) {
        System.out.println(isPalindrome(
                new ListNode(1,
                             new ListNode(2,
                                          new ListNode(2,
                                                       new ListNode(1))))));

        System.out.println(isPalindrome(
                new ListNode(1,
                             new ListNode(2,
                                          new ListNode(2,
                                                       new ListNode(3))))));
    }
}
