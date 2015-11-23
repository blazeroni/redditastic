package com.blazeroni.reddit.util;

import com.blazeroni.reddit.widget.BuildConfig;

public class Log {
    private static final String TAG = "redditastic";

    public static final boolean DEBUG = BuildConfig.DEBUG;

    public static void verbose(String message, Throwable t) {
        android.util.Log.v(TAG, message, t);
    }

    public static void verbose(String message) {
        android.util.Log.v(TAG, message);
    }

    public static void debug(String message, Throwable t) {
        android.util.Log.d(TAG, message, t);
    }

    public static void debug(String message) {
        android.util.Log.d(TAG, message);
    }

    public static void info(String message, Throwable t) {
        android.util.Log.i(TAG, message, t);
    }

    public static void info(String message) {
        android.util.Log.i(TAG, message);
    }

    public static void warn(String message, Throwable t) {
        android.util.Log.e(TAG, message, t);
    }

    public static void warn(String message) {
        android.util.Log.e(TAG, message);
    }

    public static void error(String message, Throwable t) {
        android.util.Log.e(TAG, message, t);
    }

    public static void error(String message) {
        android.util.Log.e(TAG, message);
    }
}
