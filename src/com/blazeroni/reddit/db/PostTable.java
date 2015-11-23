package com.blazeroni.reddit.db;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.blazeroni.reddit.db.WidgetTable.WidgetColumns;

public class PostTable {
    public static final String TABLE_NAME = "post";

    public interface PostColumns extends BaseColumns {
        public static final String POST_ID = "post_id";
        public static final String WIDGET_ID = "widget_id";
        public static final String ORDER = "post_order";
        public static final String TITLE = "title";
        public static final String SUBREDDIT = "subreddit";
        public static final String DOMAIN = "domain";
        public static final String POST_URL = "post_url";
        public static final String COMMENTS_URL = "comments_url";
        public static final String THUMBNAIL_URL = "thumbnail_url";
        public static final String IS_SELF = "is_self";
        public static final String SCORE = "score";
        public static final String NSFW = "nsfw";
        public static final String CREATED = "created";
        public static final String AUTHOR = "author";
        public static final String NUMBER_COMMENTS = "number_comments";
    }

    private static final String SQL_CREATE_TABLE =
        "CREATE TABLE " + TABLE_NAME + " (" +
            PostColumns.POST_ID + " TEXT," +
            PostColumns.WIDGET_ID + " INTEGER," +
            PostColumns.ORDER + " INTEGER," +
            PostColumns.TITLE + " TEXT," +
            PostColumns.SUBREDDIT + " TEXT," +
            PostColumns.DOMAIN + " TEXT," +
            PostColumns.POST_URL + " TEXT," +
            PostColumns.COMMENTS_URL + " TEXT," +
            PostColumns.THUMBNAIL_URL + " TEXT," +
            PostColumns.IS_SELF + " INTEGER," +
            PostColumns.SCORE + " INTEGER," +
            PostColumns.NSFW + " TEXT," +
            PostColumns.CREATED + " INTEGER," +
            PostColumns.AUTHOR + " TEXT," +
            PostColumns.NUMBER_COMMENTS + " INTEGER," +
            "FOREIGN KEY (" + PostColumns.WIDGET_ID + ") REFERENCES " + WidgetTable.TABLE_NAME + "(" + WidgetColumns.WIDGET_ID + ") ON DELETE CASCADE" +
        ");";

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(SQL_CREATE_TABLE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        // empty
    }
}
