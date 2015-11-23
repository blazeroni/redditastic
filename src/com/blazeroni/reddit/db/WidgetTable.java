package com.blazeroni.reddit.db;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class WidgetTable {
    public static final String TABLE_NAME = "widget";

    public interface WidgetColumns extends BaseColumns {
        public static final String WIDGET_ID = "widget_id";
        public static final String SUBREDDIT = "widget_subreddit";
        public static final String NAME = "name";
        public static final String SORT = "sort";
    }

    private static final String SQL_CREATE_TABLE =
        "CREATE TABLE " + TABLE_NAME + " (" +
            WidgetColumns.WIDGET_ID + " INTEGER PRIMARY KEY," +
            WidgetColumns.SUBREDDIT + " TEXT," +
            WidgetColumns.NAME + " TEXT," +
            WidgetColumns.SORT + " INTEGER" +
        ");";

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(SQL_CREATE_TABLE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        // empty
    }
}
