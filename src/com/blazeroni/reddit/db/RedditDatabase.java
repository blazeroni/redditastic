package com.blazeroni.reddit.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.blazeroni.reddit.db.PostTable.PostColumns;
import com.blazeroni.reddit.db.WidgetTable.WidgetColumns;
import com.blazeroni.reddit.model.Post;
import com.blazeroni.reddit.model.PostSort;
import com.blazeroni.reddit.model.WidgetInfo;
import com.blazeroni.reddit.util.Log;
import com.blazeroni.reddit.widget.R;
import com.blazeroni.reddit.widget.WidgetService;

public class RedditDatabase extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "redditastic";
    private static final int DATABASE_VERSION = 1;

    private static final String WIDGET_SELECTION = WidgetColumns.WIDGET_ID + "=?";
    private static final String POST_SELECTION = PostColumns.WIDGET_ID + "=?";
    private static final String POST_ORDER_BY = PostColumns.ORDER + " ASC";

    private static final String[] WIDGET_PROJECTION = new String[] { WidgetColumns.WIDGET_ID, WidgetColumns.NAME, WidgetColumns.SUBREDDIT, WidgetColumns.SORT };
    private static final String[] POST_ID_PROJECTION = new String[] { PostColumns.POST_ID };

    private SQLiteDatabase cached;

    public RedditDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        WidgetTable.onCreate(database);
        PostTable.onCreate(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        WidgetTable.onUpgrade(database, oldVersion, newVersion);
        PostTable.onUpgrade(database, oldVersion, newVersion);
    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase database = super.getWritableDatabase();
        if (this.cached != database) {
            enableForeignKeys(database);
            this.cached = database;
        }
        return database;
    }

    public void save(int widgetId, String name, String subreddit, PostSort sort) {
        SQLiteDatabase database = getWritableDatabase();

        database.beginTransaction();

        InsertHelper helper = new InsertHelper(database, WidgetTable.TABLE_NAME);
        try {
            ContentValues values = new ContentValues();
            values.put(WidgetColumns.WIDGET_ID, widgetId);
            values.put(WidgetColumns.NAME, name);
            values.put(WidgetColumns.SUBREDDIT, subreddit);
            values.put(WidgetColumns.SORT, sort.intValue());

            helper.replace(values);

            database.setTransactionSuccessful();
        } finally {
            helper.close();
            database.endTransaction();
        }
    }

    public void save(List<Post> posts, int widgetId) {
        SQLiteDatabase database = getWritableDatabase();

        database.beginTransaction();

        InsertHelper helper = new InsertHelper(database, PostTable.TABLE_NAME);
        try {
            database.delete(PostTable.TABLE_NAME, POST_SELECTION, new String[] { Integer.toString(widgetId) });

            ContentValues values = new ContentValues();
            int count = posts.size();
            for (int i = 0; i < count; i++) {
                values.clear();

                Post post = posts.get(i);
                values.put(PostColumns.POST_ID, post.getId());
                values.put(PostColumns.WIDGET_ID, widgetId);
                values.put(PostColumns.ORDER, i);
                values.put(PostColumns.TITLE, post.getTitle());
                values.put(PostColumns.SUBREDDIT, post.getSubreddit());
                values.put(PostColumns.DOMAIN, post.getDomain());
                if (!post.isSelfPost()) {
                    values.put(PostColumns.POST_URL, post.getUrl());
                }
                String commentsUrl = post.getPostLink();
                if (commentsUrl != null && commentsUrl.startsWith("/")) {
                    commentsUrl = "http://www.reddit.com" + commentsUrl;
                }
                values.put(PostColumns.COMMENTS_URL, commentsUrl);
                String thumbnailUrl = post.getThumbnail();
                if (thumbnailUrl != null && thumbnailUrl.startsWith("http")) {
                    values.put(PostColumns.THUMBNAIL_URL, thumbnailUrl);
                }
                values.put(PostColumns.IS_SELF, post.isSelfPost());
                values.put(PostColumns.SCORE, post.getScore());
                values.put(PostColumns.NSFW, post.isNsfw());
                values.put(PostColumns.CREATED, post.getCreated());
                values.put(PostColumns.AUTHOR, post.getAuthor());
                values.put(PostColumns.NUMBER_COMMENTS, post.getNumComments());

                helper.insert(values);
            }
            database.setTransactionSuccessful();
        } finally {
            helper.close();
            database.endTransaction();
        }
    }

    public void deleteWidget(int widgetId) {
        SQLiteDatabase database = getWritableDatabase();
        database.delete(WidgetTable.TABLE_NAME, WIDGET_SELECTION, new String[] { Integer.toString(widgetId) });
    }

    public void deletePosts(int widgetId) {
        SQLiteDatabase database = getWritableDatabase();
        database.delete(PostTable.TABLE_NAME, POST_SELECTION, new String[] { Integer.toString(widgetId) });
    }

    public boolean hasPosts(int widgetId) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(PostTable.TABLE_NAME, POST_ID_PROJECTION, POST_SELECTION, new String[] { Integer.toString(widgetId) }, null, null, null, "1");
        try {
            return cursor.getCount() > 0;
        } finally {
            cursor.close();
        }
    }

    public Cursor fetchPosts(String[] columns, int widgetId) {
        SQLiteDatabase database = getReadableDatabase();
        return database.query(PostTable.TABLE_NAME, columns, POST_SELECTION, new String[] { Integer.toString(widgetId) }, null, null, POST_ORDER_BY);
    }

    public WidgetInfo fetchWidget(int widgetId) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(WidgetTable.TABLE_NAME, WIDGET_PROJECTION, WIDGET_SELECTION, new String[] { Integer.toString(widgetId) }, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                WidgetInfo info = new WidgetInfo();
                info.widgetId = cursor.getInt(0);
                info.name = cursor.getString(1);
                info.subreddit = cursor.getString(2);
                info.sort = cursor.getInt(3);

                return info;
            }
        } finally {
            cursor.close();
        }

        return null;
    }
    
    public List<WidgetInfo> fetchAllWidgets() {
    	SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(WidgetTable.TABLE_NAME, WIDGET_PROJECTION, null, null, null, null, null);
        ArrayList<WidgetInfo> result = new ArrayList<WidgetInfo>();
        try {
        	while (cursor.moveToNext()) {
        		 WidgetInfo info = new WidgetInfo();
                 info.widgetId = cursor.getInt(0);
                 info.name = cursor.getString(1);
                 info.subreddit = cursor.getString(2);
                 info.sort = cursor.getInt(3);
                 result.add(info);
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public Cursor fetchAllWidgets(String[] columns) {
        SQLiteDatabase database = getReadableDatabase();
        return database.query(WidgetTable.TABLE_NAME, columns, null, null, null, null, null);
    }

    private void enableForeignKeys(SQLiteDatabase database) {
        try {
            database.execSQL("PRAGMA foreign_keys = ON;");
        } catch (Exception e) {
            Log.error("Failed enabling foreign keys", e);
        }
    }
}
