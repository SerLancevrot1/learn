package ru.learn.leet.code;

import java.util.ArrayList;
import java.util.List;

public class ValidPalindrome {


    public static boolean isPalindrome(String s) {
        List<Character> alphabet = new ArrayList<>(List.of('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
                                                           'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2',
                                                           '3', '4', '5', '6', '7', '8', '9'));
        var chars = s.toUpperCase().toCharArray();

        for(int i = 0; i < chars.length; i ++) {
            if (!alphabet.contains(chars[i])) {
                chars[i] = '!';
            }
        }
        String s1 = String.copyValueOf(chars).replace("!", "");
        if (s1.equals(new StringBuilder(s1).reverse().toString())) {
            return true;
        } else {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println(isPalindrome("A man, a plan, a canal: Panama"));
    }
}
