package ru.learn.leet.code.easy;

public class RemoveDuplicatesFromSortedArray {

    public static int removeDuplicates(int[] nums) {
        int l = 0;
        int r = 1;
        while (r < nums.length) {
            if (nums[l] != nums[r]) {
                l++;
                nums[l] = nums[r];

            }
            r++;
        }
        return l + 1;
    }

    public static void main(String[] args) {
        System.out.println(removeDuplicates(new int[]{1, 1, 2}));
        System.out.println(removeDuplicates(new int[]{1, 1, 2, 2, 3, 3, 3, 3, 4 ,5 ,6}));
        //                                            1, 2, 3, 4, 5, 6
        System.out.println(removeDuplicates(new int[]{1, 1, 2}));
    }
}
