package ru.learn.leet.code.easy;

import java.util.HashMap;

public class LongestSubstringWithoutRepeatingCharacters {

    public static int lengthOfLongestSubstring(String s) {
        HashMap<Character, Integer> foundedChars = new HashMap<>();
        int result = 0;
        char[] allChars = s.toCharArray();
        for (int i = 0; i < allChars.length; i++) {
            char curr = allChars[i];
            if (foundedChars.containsKey(curr)) {
                var index = foundedChars.get(curr);
                result = Math.max(i - index, result);
            }
            foundedChars.put(curr, i);

        }
        return result;
    }

    public static void main(String[] args) {
        var res = lengthOfLongestSubstring("pwwkew");
        System.out.println(res);
    }
}
