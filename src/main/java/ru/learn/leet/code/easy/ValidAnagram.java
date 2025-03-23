package ru.learn.leet.code.easy;

import java.util.ArrayList;
import java.util.List;

public class ValidAnagram {

    public static boolean isAnagram(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }
        List<Character> poolChars = new ArrayList<>(t.chars().mapToObj(c -> (char)c).toList());
        var chars = s.toCharArray();

        for(int i = 0; i < chars.length; i++) {
            if (poolChars.contains(chars[i])) {
                poolChars.remove(poolChars.indexOf(chars[i]));
            }
        }

        return poolChars.isEmpty();
//-------------------------------------------------------------------------------------------------------------------------
/*
        char[] sChars = s.toCharArray();
        char[] tChars = t.toCharArray();
        Arrays.sort(sChars);
        Arrays.sort(tChars);
        return Arrays.equals(sChars, tChars);
//-------------------------------------------------------------------------------------------------------------------------
 */
    }

    public static void main(String[] args) {
        System.out.println(isAnagram("anagram", "nagaram"));
        System.out.println(isAnagram("a", "ab"));
    }
}
