package com.blazeroni.reddit.widget;

import static android.content.Intent.ACTION_VIEW;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.widget.RemoteViews;

import com.blazeroni.reddit.RedditApp;
import com.blazeroni.reddit.activity.Main;
import com.blazeroni.reddit.db.WidgetTable.WidgetColumns;
import com.blazeroni.reddit.model.WidgetInfo;
import com.blazeroni.reddit.util.Device;
import com.blazeroni.reddit.util.Log;
import com.blazeroni.reddit.util.Preferences;
import com.blazeroni.reddit.util.Subreddits;
import com.commonsware.cwac.wakeful.WakefulIntentService;

public class WidgetService extends IntentService {
    private static final String[] PROJECTION = new String[] { WidgetColumns.WIDGET_ID + " AS " + BaseColumns._ID, WidgetColumns.SUBREDDIT };

    private static final String ACTION_MANUAL_REFRESH = "com.blazeroni.redditastic.widget.MANUAL_REFRESH";
    private static final String ACTION_UPDATE_VIEW = "com.blazeroni.redditastic.widget.UPDATE_VIEW";
    private static final String ACTION_AUTO_REFRESH = "com.blazeroni.redditastic.widget.AUTO_REFRESH";
    private static final String ACTION_EDIT = "com.blazeroni.redditastic.widget.EDIT";
    private static final String ACTION_CLICK = "com.blazeroni.redditastic.widget.CLICK";
    private static final String ACTION_CANCEL = "com.blazeroni.redditastic.widget.CANCEL";
    private static final String ACTION_OPEN_MENU = "com.blazeroni.redditastic.widget.OPEN_MENU";
    private static final String ACTION_CLOSE_MENU = "com.blazeroni.redditastic.widget.CLOSE_MENU";
    private static final String ACTION_SETTINGS = "com.blazeroni.redditastic.widget.SETTINGS";
    private static final String ACTION_REDIRECT = "com.blazeroni.redditastic.widget.REDIRECT";
    private static final String EXTRA_SUBREDDIT = "com.blazeroni.reddit.SUBREDDIT";
    private static final String EXTRA_POST_URL = "com.blazeroni.reddit.POST_URL";
    private static final String EXTRA_COMMENTS_URL = "com.blazeroni.reddit.COMMENTS_URL";
    private static final String PARAM_LAYOUT_ID = "layoutId";

    public WidgetService() {
        super("WidgetService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (Log.DEBUG) {
            Log.debug("WidgetService.onHandleIntent: " + intent.getDataString());
        }
        int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // ignore
            return;
        }

        String action = intent.getAction();
        Uri data = intent.getData();
        if (ACTION_MANUAL_REFRESH.equals(action)) {
            handleRefresh(widgetId, Integer.parseInt(data.getQueryParameter(PARAM_LAYOUT_ID)));
        } else if (ACTION_CLICK.equals(action)) {
            handleClick(widgetId, intent);
        } else if (ACTION_OPEN_MENU.equals(action)) {
            handleOpenMenu(widgetId, Integer.parseInt(data.getQueryParameter(PARAM_LAYOUT_ID)));
        } else if (ACTION_CLOSE_MENU.equals(action)) {
            handleCloseMenu(widgetId, Integer.parseInt(data.getQueryParameter(PARAM_LAYOUT_ID)));
        } else if (ACTION_CANCEL.equals(action)) {
            handleCancel(widgetId);
        } else if (ACTION_REDIRECT.equals(action)) {
            handleRedirect(widgetId, intent);
        } else if (ACTION_SETTINGS.equals(action)) {
            handleSettings(widgetId, Integer.parseInt(data.getQueryParameter(PARAM_LAYOUT_ID)));
        } else if (ACTION_UPDATE_VIEW.equals(action)) {
            handleUpdateView(widgetId);
        } else {
            handleDefault(widgetId);
        }
    }

    @TargetApi(14)
    private void handleDefault(int widgetId) {
        if (Log.DEBUG) {
            Log.debug("Widget ID: " + widgetId);
        }

        WidgetInfo info = RedditApp.getDatabase().fetchWidget(widgetId);

        // Android seems to like to launch this
        if (info == null) {
            return;
        }

        RedditApp.getDatabase().deletePosts(widgetId);

        Resources resources = getResources();
        int layoutId = Preferences.theme().getMainLayoutResource();

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        RemoteViews remote = new RemoteViews(getPackageName(), layoutId);
        remote.setTextViewText(R.id.subreddit, Subreddits.name(info.name, info.subreddit));
        remote.setOnClickPendingIntent(R.id.subreddit, viewSubredditIntent(this, info.subreddit));
        setListBackgroundColor(remote, resources);
        setAccentColor(remote,resources);
        showClosedMenu(remote);

        remote.setOnClickPendingIntent(R.id.open, openMenuIntent(this, widgetId, layoutId));
        remote.setOnClickPendingIntent(R.id.close, closeMenuIntent(this, widgetId, layoutId));
        remote.setOnClickPendingIntent(R.id.refresh, refreshPendingIntent(this, widgetId, layoutId));
        remote.setOnClickPendingIntent(R.id.settings, settingsIntent(this, widgetId, layoutId));
        remote.setOnClickPendingIntent(R.id.cancel, cancelIntent(this, widgetId));

        Intent adapterIntent = ListService.intent(this, info);
        if (Build.VERSION.SDK_INT >= ICE_CREAM_SANDWICH) {
            remote.setRemoteAdapter(R.id.list, adapterIntent);
        } else {
            remote.setRemoteAdapter(info.widgetId, R.id.list, adapterIntent);
        }

        remote.setEmptyView(R.id.list, R.id.empty);
        remote.setPendingIntentTemplate(R.id.list, createPendingIntentTemplate(this, widgetId, new Intent(this, WidgetService.class)));

        manager.updateAppWidget(widgetId, remote);

        WakefulIntentService.sendWakefulWork(this, PostService.createRefreshIntent(this, info));
    }

    @TargetApi(14)
    private void handleUpdateView(int widgetId) {
        if (Log.DEBUG) {
            Log.debug("Widget ID: " + widgetId);
        }

        WidgetInfo info = RedditApp.getDatabase().fetchWidget(widgetId);

        // Android seems to like to launch this
        if (info == null) {
            return;
        }

        if (!RedditApp.getDatabase().hasPosts(widgetId)) {
            displayError(info);
            return;
        }

        Resources resources = getResources();
        int layoutId = Preferences.theme().getMainLayoutResource();

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        RemoteViews remote = new RemoteViews(getPackageName(), layoutId);
        remote.setTextViewText(R.id.subreddit, Subreddits.name(info.name, info.subreddit));
        remote.setOnClickPendingIntent(R.id.subreddit, viewSubredditIntent(this, info.subreddit));
        setListBackgroundColor(remote, resources);
        setAccentColor(remote,resources);
        showClosedMenu(remote);

        remote.setOnClickPendingIntent(R.id.open, openMenuIntent(this, widgetId, layoutId));
        remote.setOnClickPendingIntent(R.id.close, closeMenuIntent(this, widgetId, layoutId));
        remote.setOnClickPendingIntent(R.id.refresh, refreshPendingIntent(this, widgetId, layoutId));
        remote.setOnClickPendingIntent(R.id.settings, settingsIntent(this, widgetId, layoutId));
        remote.setOnClickPendingIntent(R.id.cancel, cancelIntent(this, widgetId));

        Intent adapterIntent = ListService.intent(this, info);
        if (Build.VERSION.SDK_INT >= ICE_CREAM_SANDWICH) {
            remote.setRemoteAdapter(R.id.list, adapterIntent);
        } else {
            remote.setRemoteAdapter(info.widgetId, R.id.list, adapterIntent);
        }

        remote.setEmptyView(R.id.list, R.id.empty);
        remote.setPendingIntentTemplate(R.id.list, createPendingIntentTemplate(this, widgetId, new Intent(this, WidgetService.class)));

        manager.updateAppWidget(widgetId, remote);
    }

    // TODO refactor because this is the same as in PostService
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

    private void handleRefresh(int widgetId, int layoutId) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);

        WidgetInfo info = RedditApp.getDatabase().fetchWidget(widgetId);
        RedditApp.getDatabase().deletePosts(widgetId);
        int themeLayout = Preferences.theme().getMainLayoutResource();

        if (layoutId != themeLayout) {
            handleDefault(widgetId);
            return;
        }

        // update name due to edit
        RemoteViews remote = new RemoteViews(getPackageName(), themeLayout);
        remote.setTextViewText(R.id.subreddit, Subreddits.name(info.name, info.subreddit));
        showClosedMenu(remote);

        manager.partiallyUpdateAppWidget(widgetId, remote);
        manager.notifyAppWidgetViewDataChanged(widgetId, R.id.list);

        startService(PostService.createRefreshIntent(this, info));
    }

    private void handleClick(int widgetId, Intent intent) {
        String postUrl = intent.getStringExtra(EXTRA_POST_URL);
        String commentsUrl = intent.getStringExtra(EXTRA_COMMENTS_URL);

        if (postUrl == null) {
            Intent commentsIntent = new Intent(ACTION_VIEW, Uri.parse(commentsUrl));
            commentsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(commentsIntent);
            return;
        }

        int themeLayout = Preferences.theme().getMainLayoutResource();

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        RemoteViews remote = new RemoteViews(getPackageName(), themeLayout);

        int clickAction = Preferences.clickAction();

        switch (clickAction) {
            case Preferences.CLICK_ACTION_VIEW_LINK:
                Intent linkIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(postUrl));
                linkIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(linkIntent);
                break;
            case Preferences.CLICK_ACTION_VIEW_COMMENTS:
                Intent commentsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(commentsUrl));
                commentsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(commentsIntent);
                break;
            default:
            case Preferences.CLICK_ACTION_PROMPT:
                showActiveMenu(remote);
                remote.setViewVisibility(R.id.overlay, VISIBLE);
                remote.setOnClickPendingIntent(R.id.link_button, redirectPendingIntent(this, widgetId, postUrl));
                remote.setOnClickPendingIntent(R.id.comments_button, redirectPendingIntent(this, widgetId, commentsUrl));
                break;
        }
        
        manager.partiallyUpdateAppWidget(widgetId, remote);
    }

    private void handleOpenMenu(int widgetId, int layoutId) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        RemoteViews remote = new RemoteViews(getPackageName(), layoutId);

        showOpenMenu(remote);
        manager.partiallyUpdateAppWidget(widgetId, remote);
    }

    private void handleCloseMenu(int widgetId, int layoutId) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        RemoteViews remote = new RemoteViews(getPackageName(), layoutId);

        showClosedMenu(remote);
        manager.partiallyUpdateAppWidget(widgetId, remote);
    }

    private void handleCancel(int widgetId) {
        int themeLayout = Preferences.theme().getMainLayoutResource();

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        RemoteViews remote = new RemoteViews(getPackageName(), themeLayout);

        remote.setViewVisibility(R.id.overlay, GONE);
        showClosedMenu(remote);
        manager.partiallyUpdateAppWidget(widgetId, remote);
    }

    private void handleSettings(int widgetId, int layoutId) {
        Intent intent = new Intent(this, WidgetConfigurationActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        RemoteViews remote = new RemoteViews(getPackageName(), layoutId);

        remote.setViewVisibility(R.id.overlay, GONE);
        showClosedMenu(remote);

        manager.partiallyUpdateAppWidget(widgetId, remote);
    }

    private void handleRedirect(int widgetId, Intent intent) {
        Intent redirect = new Intent(ACTION_VIEW, intent.getData());
        redirect.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(redirect);

        int themeLayout = Preferences.theme().getMainLayoutResource();

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        RemoteViews remote = new RemoteViews(getPackageName(), themeLayout);

        remote.setViewVisibility(R.id.overlay, GONE);
        showClosedMenu(remote);

        manager.partiallyUpdateAppWidget(widgetId, remote);
    }
    
	private static PendingIntent createPendingIntentTemplate(Context context, int widgetId, Intent intent) {
    	if (Preferences.clickAction() != Preferences.CLICK_ACTION_PROMPT && isKeyguardWidget(context, widgetId)) {
    		return PendingIntent.getActivity(context, 0, Forwarder.createForwardingIntent(context, null), PendingIntent.FLAG_UPDATE_CURRENT);
    	} else {
    		return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    	}
    }
    
	private static PendingIntent createPendingIntent(Context context, int widgetId, Intent intent) {
    	if (isKeyguardWidget(context, widgetId)) {
    		return PendingIntent.getActivity(context, 0, Forwarder.createForwardingIntent(context, intent), PendingIntent.FLAG_UPDATE_CURRENT);
    	} else {
    		return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    	}
    }
    
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    /*package*/ static boolean isKeyguardWidget(Context context, int widgetId) {
    	AppWidgetManager manager = AppWidgetManager.getInstance(context);
    	boolean forward = false;
    	if (Device.isJellyBeanPlus()) {
    		Bundle options = manager.getAppWidgetOptions(widgetId);
    		forward = options.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
    	}
    	return forward;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    /*package*/ static void showClosedMenu(RemoteViews remote) {
        remote.setViewVisibility(R.id.open, VISIBLE);
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
            remote.setViewVisibility(R.id.menu_1, GONE);
            remote.setViewVisibility(R.id.menu_2, GONE);
            remote.setDisplayedChild(R.id.menu_1, 0);
            remote.setDisplayedChild(R.id.menu_2, 0);
        } else {
            remote.setViewVisibility(R.id.settings, GONE);
            remote.setViewVisibility(R.id.refresh, GONE);
        }
        remote.setViewVisibility(R.id.close, GONE);
        remote.setViewVisibility(R.id.cancel, GONE);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    /*package*/ static void showOpenMenu(RemoteViews remote) {
        remote.setViewVisibility(R.id.open, GONE);
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
            remote.setViewVisibility(R.id.menu_1, VISIBLE);
            remote.setViewVisibility(R.id.menu_2, VISIBLE);
            remote.setDisplayedChild(R.id.menu_1, 1);
            remote.setDisplayedChild(R.id.menu_2, 1);
        } else {
            remote.setViewVisibility(R.id.settings, VISIBLE);
            remote.setViewVisibility(R.id.refresh, VISIBLE);
        }
        remote.setViewVisibility(R.id.close, VISIBLE);
        remote.setViewVisibility(R.id.cancel, GONE);
    }

    /*package*/ static void showActiveMenu(RemoteViews remote) {
        remote.setViewVisibility(R.id.open, GONE);
        remote.setViewVisibility(R.id.close, GONE);
        remote.setViewVisibility(R.id.cancel, VISIBLE);
    }

    /*package*/ static void setAccentColor(RemoteViews remote, Resources resources) {
        remote.setInt(R.id.color_bar, "setBackgroundColor", resources.getColor(Preferences.accentColorResource()));
    }

    /*package*/ static void setListBackgroundColor(RemoteViews remote, Resources resources) {
        remote.setInt(R.id.list, "setBackgroundColor", resources.getColor(Preferences.theme().getBackgroundColorResource()));
    }

    /*package*/ static void setMainViewColors(RemoteViews remote, Resources resources) {
        remote.setInt(R.id.list, "setBackgroundColor", resources.getColor(Preferences.theme().getBackgroundColorResource()));
        remote.setInt(R.id.color_bar, "setBackgroundColor", resources.getColor(Preferences.accentColorResource()));
    }

    public static void refreshFrontPageWidgets(Context context) {
        Cursor cursor = RedditApp.getDatabase().fetchAllWidgets(PROJECTION);
        try {
            while (cursor.moveToNext()) {
                int widgetId = cursor.getInt(0);
                String subreddit = cursor.getString(1);
                if ("".equals(subreddit)) {
                    context.startService(intent(context, widgetId));
                }
            }
        } finally {
            cursor.close();
        }
    }

    public static PendingIntent refreshPendingIntent(Context context, int widgetId, int layoutId) {
        return PendingIntent.getService(context, 0, refreshIntent(context, widgetId, layoutId), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static Intent refreshIntent(Context context, int widgetId, int layoutId) {
        Intent intent = new Intent(context, WidgetService.class);
        Uri data = Uri.parse("redditastic://widget/refresh?id=" + widgetId + "&layoutId=" + layoutId);
        intent.setData(data);
        intent.setAction(ACTION_MANUAL_REFRESH);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return intent;
    }

    public static Intent updateViewIntent(Context context, int widgetId) {
        Intent intent = new Intent(context, WidgetService.class);
        Uri data = Uri.parse("redditastic://widget/update_view?id=" + widgetId);
        intent.setData(data);
        intent.setAction(ACTION_UPDATE_VIEW);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return intent;
    }

    public static Intent clickIntent(Context context, int widgetId, String postUrl, String commentsUrl) {
        Intent intent = new Intent(context, WidgetService.class);
        Uri data = Uri.parse("redditastic://widget/id/#" + widgetId + "/click");
        intent.setData(data);
        intent.setAction(ACTION_CLICK);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        intent.putExtra(EXTRA_POST_URL, postUrl);
        intent.putExtra(EXTRA_COMMENTS_URL, commentsUrl);
        return intent;
    }

    public static PendingIntent redirectPendingIntent(Context context, int widgetId, String url) {
        Intent intent = new Intent(context, WidgetService.class);
        Uri data = Uri.parse(url);
        intent.setData(data);
        intent.setAction(ACTION_REDIRECT);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return createPendingIntent(context, widgetId, intent);
    }

    public static PendingIntent cancelIntent(Context context, int widgetId) {
        Intent intent = new Intent(context, WidgetService.class);
        Uri data = Uri.parse("redditastic://widget/id/#" + widgetId + "/cancel");
        intent.setData(data);
        intent.setAction(ACTION_CANCEL);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static Intent defaultIntent(Context context, int widgetId) {
        Intent intent = new Intent(context, WidgetService.class);
        Uri data = Uri.parse("redditastic://widget/id/#" + widgetId);
        intent.setData(data);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return intent;
//        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent settingsIntent(Context context, int widgetId, int layoutId) {
        Intent intent = new Intent(context, WidgetService.class);
        Uri data = Uri.parse("redditastic://widget/settings?id=" + widgetId + "&layoutId=" + layoutId);
        intent.setData(data);
        intent.setAction(ACTION_SETTINGS);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return createPendingIntent(context, widgetId, intent);
    }

    public static PendingIntent viewSubredditIntent(Context context, String subreddit) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://www.reddit.com" + subreddit + "/"));
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent openMenuIntent(Context context, int widgetId, int layoutId) {
        Intent intent = new Intent(context, WidgetService.class);
        Uri data = Uri.parse("redditastic://widget/open_menu?id=" + widgetId + "&layoutId=" + layoutId);
        intent.setData(data);
        intent.setAction(ACTION_OPEN_MENU);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent closeMenuIntent(Context context, int widgetId, int layoutId) {
        Intent intent = new Intent(context, WidgetService.class);
        Uri data = Uri.parse("redditastic://widget/close_menu?id=" + widgetId + "&layoutId=" + layoutId);
        intent.setData(data);
        intent.setAction(ACTION_CLOSE_MENU);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static Intent intent(Context context, int widgetId) {
        return new Intent(context, WidgetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
    }
}
