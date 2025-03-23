package ru.learn.leet.code.easy;

public class ReverseString {
    public static void reverseString(char[] s) {
        int left = 0;
        int right = s.length - 1;

        while (left < right) {
            char buffer = s[left];
            s[left] = s[right];
            s[right] = buffer;
            left++;
            right--;
        }
    }

    public static void main(String[] args) {
        char[] chars = new char[] {'h','e','l','l','o'};
        System.out.println(chars);
        reverseString(chars);
        System.out.println(chars);
    }
}
