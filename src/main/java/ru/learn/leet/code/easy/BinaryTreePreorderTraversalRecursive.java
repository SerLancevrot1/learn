package ru.learn.leet.code.easy;

import java.util.ArrayList;
import java.util.List;

import ru.learn.leet.code.util.TreeNode;

public class BinaryTreePreorderTraversalRecursive {

    public static List<Integer> preorderTraversal(TreeNode root) {
        List<Integer> all = new ArrayList<>();
        getVal(root, all);
        return all;
    }

    private static void getVal(TreeNode root, List<Integer> all) {
        if (root == null) return;
        all.add(root.val);
        if (root.left != null) getVal(root.left, all);
        if (root.right != null) getVal(root.right, all);
    }

    public static void main(String[] args) {
        System.out.println(preorderTraversal(new TreeNode(
                5,
                new TreeNode(3, new TreeNode(2), new TreeNode(4)),
                new TreeNode(7, new TreeNode(6), new TreeNode(9)))));
    }


}
