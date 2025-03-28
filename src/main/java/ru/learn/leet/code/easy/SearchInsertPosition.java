package ru.learn.leet.code.easy;

public class SearchInsertPosition {

    public static int searchInsert(int[] nums, int target) {

        return bs(nums, 0, nums.length - 1, target);



    }

    private static int bs(int[] nums, int start, int end, int target) {
        if (start > end) {
            return start;
        }
        int middle = start + (end - start) / 2;

        if (nums[middle] > target) {
            return bs(nums, target, start, middle - 1);
        } else if (nums[middle] < target) {
            return bs(nums, target, middle + 1, end);
        } else {
            return middle;
        }
    }

    public static void main(String[] args) {
        System.out.println(searchInsert(new int[] {1,2,3,4,5,6,7}, 3));
    }
}
