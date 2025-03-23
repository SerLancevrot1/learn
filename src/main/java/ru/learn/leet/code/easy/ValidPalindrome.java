package ru.learn.leet.code.easy;

import java.util.ArrayList;
import java.util.List;

public class ValidPalindrome {


    public static boolean isPalindrome(String s) {
        char[] chars = s.toUpperCase().toCharArray();
        List<Character> validChars = new ArrayList<>();
        for (int i = 0; i < chars.length; i++) {
            if (Character.isLetter(chars[i]) || Character.isDigit(chars[i])) {
                validChars.add(chars[i]);
            }
        }
        int left = 0;
        int right = validChars.size() - 1;

        while(left < right) {
            if (!validChars.get(left).equals(validChars.get(right))) {
                return false;
            }
            left++;
            right--;
        }
        return true;
    }

    public static void main(String[] args) {
        System.out.println(isPalindrome("A man, a plan, a canal: Panama"));
    }
}
