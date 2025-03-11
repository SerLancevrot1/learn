package ru.learn.leet.code;

public class ReverseInteger {
    public static int reverse(int x) {
        var s = new StringBuilder(String.valueOf(x)).reverse().toString();
        if(x < 0) {
            s = s.replace("-", "");
            s = "-" + s;
        }
        return Integer.parseInt(s);
    }

    public static void main(String[] args) {
        System.out.println(reverse(-123));
    }
}
