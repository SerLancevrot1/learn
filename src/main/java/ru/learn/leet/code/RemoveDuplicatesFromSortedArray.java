package ru.learn.leet.code;

public class RemoveDuplicatesFromSortedArray {

    public static int removeDuplicates(int[] nums) {
        int j = 1;
        for (int i = 1; i < nums.length; i++) {
            if (nums[i] != nums[i - 1]) {
                nums[j] = nums[i];
                j++;
            }
        }
        return j;
    }

    public static void main(String[] args) {
        System.out.println(removeDuplicates(new int[]{1, 1, 2, 2, 3, 3, 3, 3, 4 ,5 ,6}));
        //                                            1, 2, 3, 4, 5, 6
        System.out.println(removeDuplicates(new int[]{1, 1, 2}));
    }
}
