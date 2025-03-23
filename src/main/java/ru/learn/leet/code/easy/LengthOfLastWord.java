package ru.learn.leet.code.easy;

public class LengthOfLastWord {
    public static int lengthOfLastWord(String s) {
        String[] split = s.split(" ");
        return split[split.length - 1].length();
    }

    public static void main(String[] args) {
        System.out.println(lengthOfLastWord("luffy is still joyboy    "));
    }
}
