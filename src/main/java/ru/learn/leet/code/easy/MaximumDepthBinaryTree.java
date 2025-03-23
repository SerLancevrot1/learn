package ru.learn.leet.code.easy;

import ru.learn.leet.code.util.TreeNode;

public class MaximumDepthBinaryTree {

    public static int maxDepth(TreeNode root) {
        int i = 1;
        if (root == null) return 0;
        return Math.max(max(root.left,i), max(root.right,i));
    }

    public static int max(TreeNode r, int i) {
        if(r == null) return i;
        i++;
        int lMax = i;
        int rMax = i;
        if (r.left != null) {
            lMax = max(r.left, i);
        }
        if (r.right != null) {
            rMax = max(r.right, i);
        }
        return Math.max(rMax, lMax);
    }

    public static void main(String[] args) {
        System.out.println(maxDepth(new TreeNode(1, null, new TreeNode(2))));

        System.out.println(maxDepth(new TreeNode(21, new TreeNode(33, new TreeNode(44), new TreeNode(55)),
                                                    new TreeNode(38, null, new TreeNode(49, new TreeNode(30), new TreeNode(63, new TreeNode(1), new TreeNode(2)))))));
    }
}
