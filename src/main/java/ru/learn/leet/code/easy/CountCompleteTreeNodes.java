package ru.learn.leet.code.easy;

import ru.learn.leet.code.util.TreeNode;

public class CountCompleteTreeNodes {
    public static int countNodes(TreeNode root) {
        if (root == null) {
            return 0;
        }
        Integer i = new Integer(0);
        return count(root, i);
    }

    public static int count(TreeNode root, Integer i) {
        if (root == null) {
            return 0;
        }
        i++;
        if (root.left != null) {
            i = count(root.left, i);
        }
        if (root.right != null) {
            i = count(root.right, i);
        }
        return i;
    }

    public static void main(String[] args) {
        System.out.println(countNodes(new TreeNode(21, new TreeNode(33, new TreeNode(44), new TreeNode(55)),
                                                 new TreeNode(38, null, new TreeNode(49, new TreeNode(30), new TreeNode(63, new TreeNode(1), new TreeNode(2)))))));

    }
}
