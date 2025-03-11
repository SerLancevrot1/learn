package ru.learn.leet.code;

import java.util.HashMap;

public class LongestPalindromicSubstring {

    public static String longestPalindrome(String s) {
        HashMap<Character, Integer> map = new HashMap<>();
        String longest = "";
        for (int i = 0; i < s.length(); i++) {
            if(map.containsKey(s.charAt(i))){
                var j = map.get(s.charAt(i));
                var substring = s.substring(j, i + 1);
                var reverse = new StringBuilder(substring).reverse().toString();
                if(substring.equals(reverse)) {
                    longest = longest.length() > substring.length() ? longest : substring;
                }
            }
            map.put(s.charAt(i), i);
        }
        return longest;
    }

    public static void main(String[] args) {
        System.out.println(longestPalindrome("babadaaaaaaaad"));
    }
}
