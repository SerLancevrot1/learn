package ru.learn.leet.code;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class IntersectionOfTwoArrays {

    public static int[] intersection(int[] nums1, int[] nums2) {
        Arrays.sort(nums1);
        Arrays.sort(nums2);
        int i = 0;
        int j = 0;
        Set<Integer> set = new HashSet<>();
        while (i < nums1.length && j < nums2.length) {
            if (nums1[i] < nums2[j]) {
                i++;
            } else if (nums1[i] > nums2[j]) {
                j++;
            } else {
                set.add(nums1[i]);
                i++;
                j++;
            }
        }
        int[] res = new int[set.size()];
        Object[] array = set.toArray();
        for (int z = 0; z < array.length; z++) {
            res[z] = (int)array[z];
        }
        return res;

    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(intersection(new int[] { 1, 2, 3, 4, 1, 2, 3, 6 }, new int[] { 2, 6 })));
    }
}
