package ru.learn.leet.code;

public class AddTwoNumbers {

    public static ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        while(l1 != null) {
            sb1.append(l1.val);
            l1 = l1.next;
        }
        while(l2 != null) {
            sb2.append(l2.val);
            l2 = l2.next;
        }
        Integer i1 = Integer.parseInt(sb1.reverse().toString());
        Integer i2 = Integer.parseInt(sb2.reverse().toString());
        String result = String.valueOf(i1 + i2);
        ListNode head = new ListNode( result.charAt(0) - '0');
        for(int j = 1; j < result.length(); j++) {
            head = new ListNode(result.charAt(j) - '0', head);
        }
        return head;
    }

    public static void main(String[] args) {
        addTwoNumbers(new ListNode(2, new ListNode(4,new ListNode(3))), new ListNode(5, new ListNode(6,new ListNode(4))));
    }

      public static class ListNode {
          int val;
          ListNode next;

          ListNode() {
          }

          ListNode(int val) {
              this.val = val;
          }

          ListNode(int val, ListNode next) {
              this.val = val;
              this.next = next;
          }
      }
}
