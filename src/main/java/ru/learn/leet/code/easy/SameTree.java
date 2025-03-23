package ru.learn.leet.code.easy;

import ru.learn.leet.code.util.TreeNode;

public class SameTree {

    public static boolean isSameTree(TreeNode p, TreeNode q) {
        if (p == null && q == null) return true;

        if (p != null && q != null && p.val == q.val) {
            return isSameTree(p.left, q.left) && isSameTree(p.right, q.right);
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println(isSameTree(new TreeNode(5, new TreeNode(2), new TreeNode(7)),
                                      new TreeNode(5, new TreeNode(2), new TreeNode(7))));

        System.out.println(isSameTree(new TreeNode(5, new TreeNode(2), new TreeNode(7)),
                                      new TreeNode(5, new TreeNode(1), new TreeNode(7))));
    }


}
