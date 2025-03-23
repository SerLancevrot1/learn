package ru.learn.leet.code.easy;

public class ValidMountainArray {
    public static boolean validMountainArray(int[] arr) {
        int peak = 0;
        boolean up = true;

        if (arr.length < 3) return false;

        int i = 0;
        while (i < arr.length - 1) {
            if (arr[i] == arr[i + 1]) {

                return false;
            }
            if (arr[i] > arr[i + 1] && up) {
                peak++;
                up = false;
            }
            if (arr[i] < arr[i + 1]) {

            }

            i++;
        }
        return peak == 1;
    }

    public static void main(String[] args) {
        System.out.println(validMountainArray(new int[] {0,3,2, 1}));
        System.out.println(validMountainArray(new int[] {1,3,2}));
        System.out.println(validMountainArray(new int[] {3,5,5}));
    }
}
