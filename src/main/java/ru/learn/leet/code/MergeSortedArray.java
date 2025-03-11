package ru.learn.leet.code;

import java.util.Arrays;

public class MergeSortedArray {

    public static void merge(int[] nums1, int m, int[] nums2, int n) {

        /*
        int i = 0;
        int j = 0;
        int resIndex = 0;
        int[] res = new int[m + n];
        while (resIndex != m + n ) {
            if (j < 3 && nums1[i] >= nums2[j] || i == m) {
                res[resIndex] = nums2[j];
                j++;
            } else {
                res[resIndex] = nums1[i];
                i++;
            }
            resIndex++;
        }
        nums1 = res;
         */
        if(m == 0) {
            nums1 = nums2;
        }
        int mIndex = m - 1;
        int nIndex = n - 1;
        while (mIndex >= 0) {
            if (nIndex < 0 || nums1[mIndex] >= nums2[nIndex]) {
                nums1[mIndex + nIndex + 1] = nums1[mIndex];
                mIndex--;
            } else {
                nums1[mIndex + nIndex + 1] = nums2[nIndex];
                nIndex--;
            }
        }
        System.out.println(Arrays.toString(nums1));
    }

    public static void main(String[] args) {
        merge(new int[] {}, 0, new int[] {1} , 1);
        merge(new int[] {1,2,3,0,0,0}, 3, new int[] {2,5,6} , 3);
        merge(new int[] {1,2,3,0,0,0}, 3, new int[] {2,3,4} , 3);
        merge(new int[] {1,2,3,0,0,0}, 3, new int[] {5,6,7} , 3);
        merge(new int[] {5,6,7,0,0,0}, 3, new int[] {1,2,3} , 3);
    }
}
