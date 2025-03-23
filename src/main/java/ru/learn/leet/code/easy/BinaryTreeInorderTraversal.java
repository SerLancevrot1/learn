package ru.learn.leet.code.easy;

import java.util.ArrayList;
import java.util.List;

import ru.learn.leet.code.util.TreeNode;

public class BinaryTreeInorderTraversal {
    public static   List<Integer> inorderTraversal(TreeNode root) {
        List<Integer> all = new ArrayList<>();
        getVal(root, all);
        return all;
    }

    public static void getVal(TreeNode root, List<Integer> all) {
        if(root == null) return;
        if (root.left != null) {
            getVal(root.left, all);
        }
        all.add(root.val) ;
        if (root.right != null) {
            getVal(root.right, all);
        }

    }

    public static void main(String[] args) {
        System.out.println(inorderTraversal(new TreeNode(
                5,
                new TreeNode(3, new TreeNode(2), new TreeNode(4)),
                new TreeNode(7, new TreeNode(6), new TreeNode(9)))));
    }
}
