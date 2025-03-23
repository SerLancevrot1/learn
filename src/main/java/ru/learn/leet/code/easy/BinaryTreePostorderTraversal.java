package ru.learn.leet.code.easy;

import java.util.ArrayList;
import java.util.List;

import ru.learn.leet.code.util.TreeNode;

public class BinaryTreePostorderTraversal {
    public List<Integer> postorderTraversal(TreeNode root) {
        List<Integer> list = new ArrayList<>();
        travel(root, list);
        return list;
    }

    public void travel(TreeNode root, List<Integer> list) {
        if (root == null) {
            return;
        }

        if (root.left != null) {
            travel(root.left, list);
        }
        if (root.right != null) {
            travel(root.right, list);
        }
        list.add(root.val);

    }
}
