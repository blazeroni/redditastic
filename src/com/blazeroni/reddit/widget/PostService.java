package com.blazeroni.reddit.widget;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.widget.RemoteViews;

import com.blazeroni.reddit.RedditApp;
import com.blazeroni.reddit.http.Http;
import com.blazeroni.reddit.model.Post;
import com.blazeroni.reddit.model.PostSort;
import com.blazeroni.reddit.model.Posts;
import com.blazeroni.reddit.model.WidgetInfo;
import com.blazeroni.reddit.util.ImageLoader;
import com.blazeroni.reddit.util.ImageLoader.Callback;
import com.blazeroni.reddit.util.Log;
import com.blazeroni.reddit.util.Subreddits;
import com.commonsware.cwac.wakeful.WakefulIntentService;

public class PostService extends WakefulIntentService {
    private static final String EXTRA_NAME = "widget_name";
    private static final String EXTRA_SUBREDDIT = "widget_subreddit";
    private static final String EXTRA_SORT = "widget_sort";
    private static final String EXTRA_AUTO_REFRESH = "auto_refresh";

    public PostService() {
        super("PostService");
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // ignore
            return;
        }

        WidgetInfo info = new WidgetInfo();
        info.widgetId = widgetId;
        info.name = intent.getStringExtra(EXTRA_NAME);
        info.subreddit = intent.getStringExtra(EXTRA_SUBREDDIT);
        info.sort = intent.getIntExtra(EXTRA_SORT, PostSort.HOT.intValue());
        
        boolean autoRefresh = intent.getBooleanExtra(EXTRA_AUTO_REFRESH, false);

        handleRefresh(info, autoRefresh);
    }

    private void handleRefresh(WidgetInfo info, boolean autoRefresh) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);

        Posts posts = fetchPosts(info);
        if (posts == null) {
            if (autoRefresh) {
            	// do nothing
            } else {
            	displayError(info);
            }
            return;
        } else if (posts.isEmpty()) {
            displayNoPosts(info);
            return;
        }

        if (Log.DEBUG) {
            Log.debug("Has posts, getting database");
        }
        RedditApp.getDatabase().save(posts.getPosts(), info.widgetId);

        if (Log.DEBUG) {
            Log.debug("Saved posts, notifying manager");
        }
        manager.notifyAppWidgetViewDataChanged(info.widgetId, R.id.list);
    }

    private void displayError(WidgetInfo info) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        RemoteViews remote = new RemoteViews(getPackageName(), R.layout.error);
        WidgetService.setAccentColor(remote, getResources());
        WidgetService.showClosedMenu(remote);

        remote.setOnClickPendingIntent(R.id.open, WidgetService.openMenuIntent(this, info.widgetId, R.layout.error));
        remote.setOnClickPendingIntent(R.id.close, WidgetService.closeMenuIntent(this, info.widgetId, R.layout.error));
        remote.setOnClickPendingIntent(R.id.refresh, WidgetService.refreshPendingIntent(this, info.widgetId, R.layout.error));
        remote.setOnClickPendingIntent(R.id.settings, WidgetService.settingsIntent(this, info.widgetId, R.layout.error));

        remote.setTextViewText(R.id.subreddit, Subreddits.name(info.name, info.subreddit));
        manager.updateAppWidget(info.widgetId, remote);
    }

    private void displayNoPosts(WidgetInfo info) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        RemoteViews remote = new RemoteViews(getPackageName(), R.layout.no_posts);
        WidgetService.showClosedMenu(remote);

        remote.setOnClickPendingIntent(R.id.open, WidgetService.openMenuIntent(this, info.widgetId, R.layout.no_posts));
        remote.setOnClickPendingIntent(R.id.close, WidgetService.closeMenuIntent(this, info.widgetId, R.layout.no_posts));
        remote.setOnClickPendingIntent(R.id.refresh, WidgetService.refreshPendingIntent(this, info.widgetId, R.layout.no_posts));
        remote.setOnClickPendingIntent(R.id.settings, WidgetService.settingsIntent(this, info.widgetId, R.layout.no_posts));

        remote.setTextViewText(R.id.subreddit, Subreddits.name(info.name, info.subreddit));
        manager.updateAppWidget(info.widgetId, remote);
    }

    private Posts fetchPosts(WidgetInfo info) {
        InputStream stream = null;
        try {
            stream = Http.fetchSubreddit(info.subreddit, PostSort.valueOf(info.sort));
            Posts posts = RedditApp.getMapper().readValue(stream, Posts.class);
            ImageLoader loader = RedditApp.getImageLoader();
            ArrayList<Post> list = posts.getPosts();
            int count = list.size();
            final CountDownLatch latch = new CountDownLatch(count);
            Callback callback = new Callback() {
                @Override
                public void imageLoaded(String url, Drawable image) {
                    latch.countDown();
                }

                @Override
                public void imageError(String url) {
                    latch.countDown();
                    Log.warn("Failed loading image: " + url);
                }
            };
            if (Log.DEBUG) {
                Log.debug("count: " + count);
            }
            for (int i = 0; i < count; i++) {
                Post post = list.get(i);
                String url = post.getThumbnail();
                if (url != null && url.startsWith("http")) {
                    loader.load(url, callback);
                } else {
                    latch.countDown();
                }
            }
            latch.await(5, TimeUnit.SECONDS);

            return posts;
        } catch (Exception e) {
            Log.error("Failed fetching subreddit! " + e.getClass(), e);
        } finally {
            IOUtils.closeQuietly(stream);
        }

        return null;
    }

    public static Intent createRefreshIntent(Context context, WidgetInfo info) {
        return new Intent(context, PostService.class)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, info.widgetId)
            .putExtra(EXTRA_NAME, info.name)
            .putExtra(EXTRA_SUBREDDIT, info.subreddit)
            .putExtra(EXTRA_SORT, info.sort);
    }
    
    public static Intent createAutoRefreshIntent(Context context, WidgetInfo info) {
    	return createRefreshIntent(context, info)
    		.putExtra(EXTRA_AUTO_REFRESH, true);
    }
}
