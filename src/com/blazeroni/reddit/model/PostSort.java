package com.blazeroni.reddit.model;

import com.blazeroni.reddit.widget.R;

public enum PostSort {
    HOT(1, R.string.sort_whats_hot, "/", ""),
    NEW(2, R.string.sort_new_new, "/new/", "sort=new"),
    RISING(3, R.string.sort_new_rising, "/new/", "sort=rising"),
    CONTROVERSIAL_THIS_HOUR(4, R.string.sort_controversial_hour, "/controversial/", "sort=controversial&t=hour"),
    CONTROVERSIAL_TODAY(5, R.string.sort_controversial_today, "/controversial/", "sort=controversial&t=day"),
    CONTROVERSIAL_THIS_WEEK(6, R.string.sort_controversial_week, "/controversial/", "sort=controversial&t=week"),
    CONTROVERSIAL_THIS_MONTH(7, R.string.sort_controversial_month, "/controversial/", "sort=controversial&t=month"),
    // missed originally - out of order value
    CONTROVERSIAL_THIS_YEAR(14, R.string.sort_controversial_year, "/controversial/", "sort=controversial&t=year"),
    CONTROVERSIAL_ALL_TIME(8, R.string.sort_controversial_all_time, "/controversial/", "sort=controversial&t=all"),
    TOP_THIS_HOUR(9, R.string.sort_top_hour, "/top/", "sort=top&t=hour"),
    TOP_TODAY(10, R.string.sort_top_today, "/top/", "sort=top&t=day"),
    TOP_THIS_WEEK(11, R.string.sort_top_week, "/top/", "sort=top&t=week"),
    TOP_THIS_MONTH(12, R.string.sort_top_month, "/top/", "sort=top&t=month"),
    // missed originally - out of order value
    TOP_THIS_YEAR(15, R.string.sort_top_year, "/top/", "sort=top&t=year"),
    TOP_ALL_TIME(13, R.string.sort_top_all_time, "/top/", "sort=top&t=all");

    private final int value;
    private final int stringId;
    private final String path;
    private final String query;

    private PostSort(int value, int stringId, String path, String query) {
        this.value = value;
        this.stringId = stringId;
        this.path = path;
        this.query = query;
    }

    public int getStringId() {
        return this.stringId;
    }

    public String getPath() {
        return this.path;
    }

    public String getQuery() {
        return this.query;
    }

    public int intValue() {
        return this.value;
    }

    public static PostSort valueOf(int value) {
        PostSort[] values = PostSort.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].value == value) {
                return values[i];
            }
        }
        return null;
    }
}
