package ru.learn.leet.code.easy;

public class SearchInsertPosition {

    public static int searchInsert(int[] nums, int target) {

        return search(nums, 0, nums.length - 1, target, 0);

    }

    private static int search(int[] nums, int start, int end, int target, int res) {
        int mid = (end - start) / 2;
        System.out.println(start + " "+ end + " "+ target + " "+ res);


        if (nums[mid] > target) {
            res = search(nums, start, mid, target, res);
        } else if (nums[mid] < target) {
            res = search(nums, mid, end, target, res);
        } else {
            return mid;
        }
        return res;
    }

    public static void main(String[] args) {
        System.out.println(searchInsert(new int[] {1,2,3,4,5,6,7}, 3));
    }
}
