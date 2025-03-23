package ru.learn.leet.code.easy;

import ru.learn.leet.code.util.TreeNode;

public class SymmetricTree {

    public static boolean isSymmetric(TreeNode root) {
        if (root == null) return true;
        return isSymmetric(root.left, root.right);
    }

    public static boolean isSymmetric(TreeNode l, TreeNode r) {
        if (l == null && r == null) return true;
        if (l != null && r != null && l.val == r.val) {
            return isSymmetric(l.left, r.right) && isSymmetric(l.right, r.left);
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println(isSymmetric(new TreeNode(2, new TreeNode(3, new TreeNode(4), new TreeNode(5)),
                                                    new TreeNode(3, null, new TreeNode(4)))));
    }
}
