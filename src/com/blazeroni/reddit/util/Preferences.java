package com.blazeroni.reddit.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

import com.blazeroni.reddit.RedditApp;
import com.blazeroni.reddit.model.User;
import com.blazeroni.reddit.widget.R;

public class Preferences {
    public static final String WIDGET_CLICK_ACTION = "widget_click_action";
    public static final String DEFAULT_CLICK_ACTION = "1";
    public static final int CLICK_ACTION_VIEW_LINK = 0;
    public static final int CLICK_ACTION_VIEW_COMMENTS = 1;
    public static final int CLICK_ACTION_PROMPT = 2;

    public static final String THEME = "theme";
    public static final String DEFAULT_THEME = "0";
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;

    public static final String ACCENT_COLOR = "accent_color";
    public static final String DEFAULT_ACCENT_COLOR = "0";
    public static final int ACCENT_COLOR_ORANGERED = 0;
    public static final int ACCENT_COLOR_ICS_BLUE = 1;
    public static final int ACCENT_COLOR_WHITE = 2;
    public static final int ACCENT_COLOR_TRANSPARENT = 3;

    private static final int ACCENT_COLOR_ORANGERED_RESOURCE = R.color.orangered;
    private static final int ACCENT_COLOR_ICS_BLUE_RESOURCE = R.color.ics_blue;
    private static final int ACCENT_COLOR_WHITE_RESOURCE = android.R.color.white;
    private static final int ACCENT_COLOR_TRANSPARENT_RESOURCE = android.R.color.transparent;

    public static final String ACCOUNT = "account";
    public static final String LAST_SUBREDDIT_REFRESH = "last_subreddit_refresh";

    public static final String AUTO_REFRESH_ENABLED = "auto_refresh_enabled";
    public static final String REFRESH_FREQUENCY = "refresh_frequency";
    public static final int DEFAULT_REFRESH_FREQUENCY = 24;

    private static final String WIDGET_SUBREDDIT_PREFIX = "widget_subreddit_";
    private static final String WIDGET_SORT_PREFIX = "widget_sort_";

    private static final String PREF_USERNAME = "username";
    private static final String PREF_MODHASH = "modhash";
    private static final String PREF_COOKIE = "cookie";

    private static final String PREF_SUBREDDITS = "subreddits";
    
    private static final String REFRESH_NEEDED = "refresh_needed";
    
    private static final String FIRST_LAUNCH = "first_launch";

    private static SharedPreferences preferences;

    private static List<String> subreddits;

    public static void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        preferences().registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        preferences().unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static void saveUser(User user) {
        Editor editor = preferences().edit();
        editor.putString(PREF_USERNAME, user.getUsername());
        editor.putString(PREF_MODHASH, user.getModhash());
        editor.putString(PREF_COOKIE, user.getCookie());
        editor.commit();
    }

    public static User loadUser() {
        SharedPreferences prefs = preferences();
        String username = prefs.getString(PREF_USERNAME, null);
        if (username != null) {
            User user = new User();
            user.setUsername(username);
            user.setModhash(prefs.getString(PREF_MODHASH, null));
            user.setCookie(prefs.getString(PREF_COOKIE, null));
            return user;
        }
        return null;
    }

    public static void deleteUser() {
        Editor editor = preferences().edit();
        editor.remove(PREF_USERNAME);
        editor.remove(PREF_MODHASH);
        editor.remove(PREF_COOKIE);
        editor.remove(PREF_SUBREDDITS);
        editor.remove(LAST_SUBREDDIT_REFRESH);
        editor.commit();
        subreddits = Collections.emptyList();
    }

    public static void saveSubreddits(List<String> subreddits) {
        Collections.sort(subreddits, String.CASE_INSENSITIVE_ORDER);
        Preferences.subreddits = subreddits;

        HashSet<String> set = new HashSet<String>(subreddits);
        Editor editor = preferences().edit();
        editor.putStringSet(PREF_SUBREDDITS, set);
        editor.putLong(LAST_SUBREDDIT_REFRESH, System.currentTimeMillis());
        editor.commit();
    }

    public static List<String> loadSubreddits() {
        if (subreddits == null) {
            Set<String> set = preferences().getStringSet(PREF_SUBREDDITS, null);
            if (set == null) {
                return Collections.emptyList();
            }

            ArrayList<String> list = new ArrayList<String>(set);
            Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
            subreddits = list;
        }
        return subreddits;
    }

    public static void deleteSubreddits() {
        subreddits = Collections.emptyList();
        Editor editor = preferences().edit();
        editor.remove(PREF_SUBREDDITS);
        editor.remove(LAST_SUBREDDIT_REFRESH);
        editor.commit();
    }

    public static long lastSubredditRefresh() {
        return preferences().getLong(LAST_SUBREDDIT_REFRESH, 0);
    }

    public static void saveClickAction(int clickAction) {
    	Editor editor = preferences().edit();
    	editor.putString(WIDGET_CLICK_ACTION, Integer.toString(clickAction));
    	editor.commit();
    }
    
    public static int clickAction() {
        switch (preferences().getString(WIDGET_CLICK_ACTION, DEFAULT_CLICK_ACTION).charAt(0)) {
            case '0':
                return CLICK_ACTION_VIEW_LINK;
            case '1':
                return CLICK_ACTION_VIEW_COMMENTS;
            case '2':
            default:
                return CLICK_ACTION_PROMPT;
        }
    }

    public static Theme theme() {
        switch (preferences().getString(THEME, DEFAULT_THEME).charAt(0)) {
            case '1':
                return new DarkTheme();
            case '0':
            default:
                return new LightTheme();
        }
    }

    public static int accentColorResource() {
        switch (preferences().getString(ACCENT_COLOR, DEFAULT_ACCENT_COLOR).charAt(0)) {
            case '0':
            default:
                return ACCENT_COLOR_ORANGERED_RESOURCE;
            case '1':
                return ACCENT_COLOR_ICS_BLUE_RESOURCE;
            case '2':
                return ACCENT_COLOR_WHITE_RESOURCE;
            case '3':
                return ACCENT_COLOR_TRANSPARENT_RESOURCE;
        }
    }

    public static void saveAutoRefreshEnabled(boolean value) {
    	Editor editor = preferences().edit();
        editor.putBoolean(AUTO_REFRESH_ENABLED, value);
        editor.commit();
    }
    
    public static boolean autoRefreshEnabled() {
        return preferences().getBoolean(AUTO_REFRESH_ENABLED, true);
    }
    
    public static void configureIfFirstLaunch() {
    	SharedPreferences pref = preferences();
		if (pref.getBoolean(FIRST_LAUNCH, true)) {
    		Editor editor = pref.edit();
    		editor.putBoolean(AUTO_REFRESH_ENABLED, true);
    		editor.putInt(REFRESH_FREQUENCY, DEFAULT_REFRESH_FREQUENCY);
    		editor.putBoolean(FIRST_LAUNCH, false);
    		editor.commit();
    	}
    }

    public static int refreshFrequency() {
        return preferences().getInt(REFRESH_FREQUENCY, DEFAULT_REFRESH_FREQUENCY);
    }

    public static void saveRefreshFrequency(int value) {
        Editor editor = preferences().edit();
        editor.putInt(REFRESH_FREQUENCY, value);
        editor.commit();
    }
    
    public static boolean refreshNeeded() {
    	return preferences().getBoolean(REFRESH_NEEDED, false);
    }
    
    public static void saveRefreshNeeded(boolean value) {
        Editor editor = preferences().edit();
        editor.putBoolean(REFRESH_NEEDED, value);
        editor.commit();
    }

//    public static void saveWidgetSubreddit(int widgetId, String subreddit, PostSort sort) {
//        Editor editor = preferences().edit();
//        editor.putString(WIDGET_SUBREDDIT_PREFIX + widgetId, subreddit);
//        editor.putInt(WIDGET_SORT_PREFIX + widgetId, sort.intValue());
//        editor.commit();
//    }
//
//    public static String loadWidgetSubreddit(int widgetId) {
//        return preferences().getString(WIDGET_SUBREDDIT_PREFIX + widgetId, null);
//    }
//
//    public static int loadWidgetSort(int widgetId) {
//        return preferences().getInt(WIDGET_SORT_PREFIX + widgetId, PostSort.HOT.intValue());
//    }
//
//    public static void deleteWidgetSubreddit(int widgetId) {
//        preferences().edit().remove(WIDGET_SUBREDDIT_PREFIX + widgetId).commit();
//    }

//    public static void saveWidgetDestination(int widgetId, int destination) {
//        preferences().edit().putInt(WIDGET_DESTINATION_PREFIX + widgetId, destination).commit();
//    }
//
//    public static int loadWidgetDestination(int widgetId) {
//        return preferences().getInt(WIDGET_DESTINATION_PREFIX + widgetId, DESTINATION_POST);
//    }
//
//    public static void deleteWidgetDestination(int widgetId) {
//        preferences().edit().remove(WIDGET_DESTINATION_PREFIX + widgetId).commit();
//    }

//    public static void deleteWidget(int widgetId) {
//        Editor edit = preferences().edit();
//        edit.remove(WIDGET_SUBREDDIT_PREFIX);
//        edit.remove(WIDGET_DESTINATION_PREFIX);
//        edit.commit();
//    }

    private static SharedPreferences preferences() {
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(RedditApp.getContext());
        }
        return preferences;
    }
}
