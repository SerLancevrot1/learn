package ru.learn.leet.code.easy;

public class BinarySearch {

    public static int binarySearch(int[] arr, int target) {
        return bs(arr, target, 0, arr.length - 1);
    }

    private static int bs(int[] arr, int target, int start, int end) {
        int middle = (end - start) / 2;

        if (arr[middle] > target) {
            return bs(arr, target, start, middle);
        } else if (arr[middle] < target) {
            return bs(arr, target, middle, end);
        } else {
            return middle;
        }
    }

    public static void main(String[] args) {
        System.out.println(binarySearch(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, 9));
    }
}
