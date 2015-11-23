package com.blazeroni.reddit.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class User {
    private static final String PREF_USERNAME = "username";
    private static final String PREF_MODHASH = "modhash";
    private static final String PREF_COOKIE = "cookie";

    private String username;
    private String modhash;
    private String cookie;

    public static User load(Context context, String username) {
        SharedPreferences prefs = context.getSharedPreferences("user-" + username, Context.MODE_PRIVATE);
        if (!prefs.contains(PREF_USERNAME)) {
            return null;
        }

        User user = new User();
        user.username = username;
        user.modhash = prefs.getString(PREF_MODHASH, null);
        user.cookie = prefs.getString(PREF_COOKIE, null);

        return user;
    }

    public static void save(Context context, User user) {
        if (user.username == null) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("user-" + user.username, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(PREF_USERNAME, user.username);
        editor.putString(PREF_MODHASH, user.modhash);
        editor.putString(PREF_COOKIE, user.cookie);
        editor.commit();
    }

    public boolean isLoggedIn() {
        return this.username != null && this.modhash != null && this.cookie != null;
    }

    public String getUsername() {
        return this.username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getModhash() {
        return this.modhash;
    }
    public void setModhash(String modhash) {
        this.modhash = modhash;
    }
    public String getCookie() {
        return this.cookie;
    }
    public void setCookie(String cookie) {
        this.cookie = cookie;
    }
}
