package ru.learn.leet.code;

import java.util.Arrays;

public class MoveZeroes {

    public static void moveZeroes(int[] nums) {
        int l = 0;
        int r = 0;
        while (l < nums.length) {
            if (nums[l] != 0) { // 1 0 0 3 1
                swap(nums, l, r);
                r++;
            }
            l++;
        }

        System.out.println(Arrays.toString(nums));
    }

    private static void swap(int[] nums, int i, int j){
        int k = nums[i];
        nums[i] = nums[j];
        nums[j] = k;

    }

    public static void main(String[] args) {
        moveZeroes(new int[] { 0, 1, 0, 3, 12 });
    }
}
