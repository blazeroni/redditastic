package com.blazeroni.reddit.widget;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import org.apache.commons.lang3.StringUtils;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.blazeroni.reddit.RedditApp;
import com.blazeroni.reddit.db.PostTable.PostColumns;
import com.blazeroni.reddit.model.WidgetInfo;
import com.blazeroni.reddit.util.Log;
import com.blazeroni.reddit.util.Preferences;
import com.blazeroni.reddit.util.Theme;

public class ListService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        return new PostRemoteViewsFactory(getApplicationContext(), widgetId);
    }

    public static Intent intent(Context context, WidgetInfo info) {
        Uri data = Uri.parse("redditastic://widget/id/#" + info.widgetId);
        return new Intent(context, ListService.class).setData(data).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, info.widgetId);
    }
}

class PostRemoteViewsFactory implements RemoteViewsFactory, OnSharedPreferenceChangeListener {
    private static final String[] PROJECTION = new String[] { PostColumns.TITLE,
                                                              PostColumns.SCORE,
                                                              PostColumns.NUMBER_COMMENTS,
                                                              PostColumns.DOMAIN,
                                                              PostColumns.POST_URL,
                                                              PostColumns.COMMENTS_URL,
                                                              PostColumns.THUMBNAIL_URL,
                                                              PostColumns.IS_SELF };

    private final Context context;
    private final int widgetId;
    private Cursor cursor;

    private Theme theme;
    private int cachedTextColor;
    private int cachedSecondaryTextColor;

    public PostRemoteViewsFactory(Context context, int widgetId) {
        this.context = context;
        this.widgetId = widgetId;
    }

    @Override
    public void onCreate() {
        Preferences.registerOnSharedPreferenceChangeListener(this);
        updateTheme(Preferences.theme());
    }

    @Override
    public void onDestroy() {
        Preferences.unregisterOnSharedPreferenceChangeListener(this);
        if (this.cursor != null) {
            this.cursor.close();
        }
    }

    @Override
    public void onDataSetChanged() {
        if (this.cursor != null) {
            this.cursor.close();
        }

        if (Log.DEBUG) {
            Log.debug("fetching posts");
        }
        this.cursor = RedditApp.getDatabase().fetchPosts(PROJECTION, this.widgetId);
    }

    @Override
    public int getCount() {
        if (Log.DEBUG) {
            Log.debug("" + (this.cursor != null ? this.cursor.getCount() : 0));
        }
        return this.cursor != null ? this.cursor.getCount() : 0;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        String title = "Error";
        String score = "";
        String numComments = "";
        String domain = "";
        String postUrl = "";
        String commentsUrl = "";
        String thumbnailUrl = "";
        boolean self = false;

        if (this.cursor.moveToPosition(position)) {
            title = this.cursor.getString(0);
            score = this.cursor.getString(1);
            numComments = this.cursor.getString(2);
            domain = this.cursor.getString(3);
            postUrl = this.cursor.getString(4);
            commentsUrl = this.cursor.getString(5);
            thumbnailUrl = this.cursor.getString(6);
            self = this.cursor.getInt(7) != 0;
        }

        RemoteViews remote = new RemoteViews(this.context.getPackageName(), R.layout.post_list_item);
        remote.setTextViewText(R.id.title, title);
        remote.setTextViewText(R.id.score, score);
        remote.setTextViewText(R.id.comments, numComments);
        remote.setTextViewText(R.id.domain, domain);

        remote.setTextColor(R.id.title, this.cachedTextColor);

        remote.setTextColor(R.id.score, this.cachedSecondaryTextColor);
        remote.setTextColor(R.id.comments, this.cachedSecondaryTextColor);
        remote.setTextColor(R.id.domain, this.cachedSecondaryTextColor);

        // fix for reddit bug
        commentsUrl = commentsUrl.replace("//comments", "/comments");
        
        Intent intent = WidgetService.clickIntent(this.context, this.widgetId, postUrl, commentsUrl);
        if (Preferences.clickAction() != Preferences.CLICK_ACTION_PROMPT && WidgetService.isKeyguardWidget(context, widgetId)) {
        	intent = Forwarder.createForwardingIntent(context, intent);
        }

        remote.setOnClickFillInIntent(R.id.container, intent);

        remote.setViewVisibility(R.id.thumbnail, GONE);

        if (StringUtils.isNotBlank(thumbnailUrl)) {
            Drawable drawable = RedditApp.getImageLoader().get(thumbnailUrl);
            if (drawable != null && drawable instanceof BitmapDrawable) {
                remote.setImageViewBitmap(R.id.thumbnail, ((BitmapDrawable) drawable).getBitmap());
                remote.setViewVisibility(R.id.thumbnail, VISIBLE);
            }
        }

        return remote;
    }

    @Override
    public RemoteViews getLoadingView() {
        return new RemoteViews(this.context.getPackageName(), this.theme.getListLoadingViewResource());
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private void updateTheme(Theme theme) {
        this.theme = theme;
        this.cachedTextColor = this.context.getResources().getColor(this.theme.getTextColorResource());
        this.cachedSecondaryTextColor = this.context.getResources().getColor(this.theme.getSecondaryTextColorResource());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Preferences.THEME)) {
            updateTheme(Preferences.theme());
        }
    }
}
