package com.blazeroni.reddit.util;

import java.util.Arrays;
import java.util.List;

import com.blazeroni.reddit.RedditApp;
import com.blazeroni.reddit.widget.R;

public class Subreddits {
    private static final String[] DEFAULT_SUBREDDITS = new String[] {
        "AdviceAnimals",
        "Android",
        "announcements",
        "AskReddit",
        "askscience",
        "atheism",
        "aww",
        "bestof",
        "blog",
        "books",
        "comics",
        "DoesAnybodyElse",
        "fffffffuuuuuuuuuuuu",
        "Fitness",
        "food",
        "Frugal",
        "funny",
        "gadgets",
        "gaming",
        "geek",
        "gifs",
        "humor",
        "IAmA",
        "movies",
        "music",
        "news",
        "offbeat",
        "pics",
        "politics",
        "programming",
        "science",
        "technology",
        "todayilearned",
        "trees",
        "videos",
        "worldnews",
        "WTF",
    };

    private Subreddits() {}

    public static List<String> subreddits() {
        List<String> list = Preferences.loadSubreddits();
        if (list == null || list.isEmpty()) {
            list = Arrays.asList(DEFAULT_SUBREDDITS);
        }
        return list;
    }

    public static String[] defaultSubreddits() {
        return DEFAULT_SUBREDDITS;
    }

    public static String name(String name, String subreddit) {
        if (name != null) {
            return name;
        } else {
            return Subreddits.defaultName(subreddit);
        }
    }

    public static String defaultName(String subreddits) {
        if (subreddits == null || "".equals(subreddits) || "/".equals(subreddits)) {
            return RedditApp.getContext().getString(R.string.front_page_default_name);
        } else if (subreddits.startsWith("/")) {
            return subreddits.substring(1);
        } else {
            return subreddits;
        }
    }
}
