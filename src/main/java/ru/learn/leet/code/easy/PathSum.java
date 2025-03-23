package ru.learn.leet.code.easy;

import ru.learn.leet.code.util.TreeNode;

public class PathSum {
    public static boolean hasPathSum(TreeNode root, int targetSum) {
        if (root == null) {
            return false;
        }
        if (root.val == targetSum && (root.left != null && root.right != null)) {
            return false;
        }
        return sum(root, targetSum, 0);
    }

    public static boolean sum(TreeNode root, int targetSum, int res) {
        int i = root.val + res;
        if (i == targetSum && root.left == null && root.right == null) {
            return true;
        }
        boolean l = false;
        if (root.left != null) {
            l = sum(root.left, targetSum, i);
        }
        boolean r = false;
        if (root.right != null) {
            r = sum(root.right, targetSum, i);
        }

        return l || r;
    }

    public static void main(String[] args) {
        System.out.println(hasPathSum(new TreeNode(0, new TreeNode(2, new TreeNode(-2), null), null), 0));
        System.out.println(hasPathSum(new TreeNode(1, new TreeNode(2), null), 1));
        System.out.println(hasPathSum(new TreeNode(5,
                                                   new TreeNode(4, new TreeNode(11, new TreeNode(11), new TreeNode(2)), null),
                                                   new TreeNode(8, new TreeNode(13), new TreeNode(4, null, new TreeNode(1)))) ,
                                      22));
    }
}
