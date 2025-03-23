package ru.learn.leet.code.easy;

import ru.learn.leet.code.util.TreeNode;

public class MinimumDepthBinaryTree {

    public int minDepth(TreeNode root) {
        if (root == null) {
            return 0;
        }
        if (root.left == null && root.right != null) {
            return minDepth(root.right) + 1;
        }
        if (root.left != null && root.right == null) {
            return minDepth(root.left) + 1;
        }
        int l = minDepth(root.left) + 1;
        int r = minDepth(root.right) + 1;
        return Math.min(l,r);
    }
}
