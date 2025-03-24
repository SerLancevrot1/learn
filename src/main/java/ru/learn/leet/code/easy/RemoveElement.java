package ru.learn.leet.code.easy;

import java.util.Arrays;

public class RemoveElement {

    public static int removeElement(int[] nums, int val) {

        int l = 0;
        int r = nums.length - 1;
        int res = 0;

        while (l < r) {
            if (nums[r] == val) {
                r--;
            } else if (nums[r] != val && nums[l] == val) {
                int temp = nums[l];
                nums[l] = nums[r];
                nums[r] = temp;
            }
            if (nums[l] == val) continue;

            l++;
        }

        for (int num : nums) {
            if (num == val)
                break;
            res++;
        }

        System.out.println(Arrays.toString(nums));
        System.out.println(res);
        return res;
    }

    public static void main(String[] args) {
        removeElement(new int[] {4,2,0,2,2,1,4,4,1,4,3,2}, 4);
//        removeElement(new int[] {0,1,2,2,3,0,4,2}, 2);
    }
}
