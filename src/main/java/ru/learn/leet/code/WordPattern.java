package ru.learn.leet.code;

import java.util.HashMap;

public class WordPattern {

    public static boolean wordPattern(String pattern, String s) {
        String[] words = s.split(" ");
        char[] chars = pattern.toCharArray();

        if (words.length != chars.length) {
            return false;
        }

        HashMap<Character, String> map = new HashMap<>();

        for (int i = 0; i < chars.length; i++) {
            if(map.containsKey(chars[i]) || map.containsValue(words[i])) {
                String mapPair = map.get(chars[i]);
                if (mapPair == null || !mapPair.equals(words[i])) {
                    return false;
                }
            } else {
                map.put(chars[i], words[i]);
            }

        }
        return true;
    }

    public static void main(String[] args) {
        System.out.println(wordPattern("abba", "dog cat cat fish"));
    }
}
