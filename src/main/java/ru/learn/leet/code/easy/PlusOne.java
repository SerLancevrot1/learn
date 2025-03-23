package ru.learn.leet.code.easy;

import java.util.Arrays;

public class PlusOne {

    public static int[] plusOne(int[] digits) {

        if (digits.length == 0)
            return digits;
        digits[digits.length - 1] = digits[digits.length - 1] + 1;
        if (digits[digits.length - 1] != 10)
            return digits;
        if (digits.length == 1)
            return new int[] { 1, 0 };
        digits[digits.length - 1] = 0;
        int point = 1;
        // 9 9 9 0
        for (int i = digits.length - 2; i >= 0; i--) {

            if (i == 0 && digits[i] == 9 && point != 0) {
                int[] res = new int[digits.length + 1];
                res[0] = 1;
                digits[0] = 0;
                for (int j = 0; j < digits.length; j++) {
                    res[j + 1] = digits[j];
                }
                return res;
            }
            if (point == 0) {
                return digits;
            } else if (digits[i] == 9) {
                digits[i] = 0;
            } else {
                digits[i] = digits[i] + 1;
                point = 0;
                return digits;
            }
        }
        return null;
    }

    /*
    for (int i = digits.length - 1; i >= 0; i--) {
	    if (digits[i] < 9) {
		    digits[i]++;
		    return digits;
	    }
	digits[i] = 0;
    }

    digits = new int[digits.length + 1];
    digits[0] = 1;
    return digits;
     */

    public static void main(String[] args) {
        System.out.println(Arrays.toString(plusOne(new int[] { 8, 9, 9, 9 })));
    }
}
