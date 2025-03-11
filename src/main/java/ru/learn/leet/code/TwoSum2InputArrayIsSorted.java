package ru.learn.leet.code;

import java.util.Arrays;

public class TwoSum2InputArrayIsSorted {

    public static int[] twoSum(int[] numbers, int target) {
        int i = 0;
        int j = numbers.length - 1;

        while (i!=j) {
            if (numbers[i] + numbers[j] > target) {
                j--;
            } else if (numbers[i] + numbers[j] < target) {
                i++;
            } else {
                return new int[] {i, j};
            }
        }
        return new int[] {-1};
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(twoSum(new int[] {1,6,7,8,9}, 13)));
    }
}
