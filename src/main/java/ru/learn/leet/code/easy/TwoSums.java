package ru.learn.leet.code.easy;

import java.util.Arrays;
import java.util.HashMap;

public class TwoSums {

    /*
Input: nums = [2,7,11,15], target = 9
Output: [0,1]
Explanation: Because nums[0] + nums[1] == 9, we return [0, 1].
    *  */

    public static int[] twoSum(int[] nums, int target) {
/*
        for (int i = 0; i < nums.length; i++) {
            for (int j = 0; j < nums.length; j++) {
                if (nums[i] + nums[j] == target && j != i) {
                    return new int[] { i, j };
                }
            }
        }
*/

//        int left = 0, right = nums.length - 1;
//        while (left < right) {
//            if (nums[left] + nums[right] == target) return new int[]{left, right};
//            else if (nums[left] + nums[right] < target) left++;
//            else if (nums[left] + nums[right] > target) right--;
//        }

        HashMap<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int x = target - nums[i];
            if (map.containsKey(x)) {
                return new int[]{map.get(x), i};
            }
            map.put(nums[i], i);
        }
        return null;
    }

    public static void main(String[] args) {
        var ints = twoSum(new int[] { 3, 2, 4 }, 6);
        assert ints[0] == 2;
        System.out.println(Arrays.toString(ints));
    }
}
