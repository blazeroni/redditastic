package com.blazeroni.reddit.util;

public class Strings {
    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static boolean notEmpty(String s) {
        return !isEmpty(s);
    }
}
