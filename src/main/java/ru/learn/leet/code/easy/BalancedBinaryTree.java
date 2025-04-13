package ru.learn.leet.code.easy;

import ru.learn.leet.code.util.TreeNode;

public class BalancedBinaryTree {

    public boolean isBalanced(TreeNode root) {
        if (root == null) return true;
        return Math.abs(bal(root.left, 1) - bal(root.right, 1)) < 2 && isBalanced(root.left) && isBalanced(root.right);
    }

    public int bal(TreeNode root, int cur) {
        if (root == null) return cur;
        return Math.max(bal(root.left, cur + 1), bal(root.right, cur + 1));
    }
}
