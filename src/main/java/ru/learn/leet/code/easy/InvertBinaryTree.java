package ru.learn.leet.code.easy;

import ru.learn.leet.code.util.TreeNode;

public class InvertBinaryTree {
    public TreeNode invertTree(TreeNode root) {
        if (root == null) {
            return root;
        }
        invert(root);
        return root;
    }

    public void invert(TreeNode root) {
        if (root == null) {
            return;
        }

        if (root.left != null && root.right == null) {
            root.right = root.left;
            root.left = null;
            invert(root.right);

        } else if (root.left == null && root.right != null) {
            root.left = root.right;
            root.right = null;
            invert(root.left);
        }


        if (root.left != null && root.right != null) {
            TreeNode temp = root.left;
            root.left = root.right;
            root.right = temp;
            invert(root.left);
            invert(root.right);
        }

    }
}
