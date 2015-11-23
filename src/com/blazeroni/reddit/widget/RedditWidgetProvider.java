package com.blazeroni.reddit.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.blazeroni.reddit.RedditApp;
import com.blazeroni.reddit.activity.Main;
import com.blazeroni.reddit.db.RedditDatabase;
import com.blazeroni.reddit.model.WidgetInfo;
import com.blazeroni.reddit.util.Log;
import com.blazeroni.reddit.util.Preferences;
import com.blazeroni.reddit.util.Refresher;

public class RedditWidgetProvider extends AppWidgetProvider {
	public static final String EXTRA_INTENT = "com.blazeroni.reddit.widget.INTENT";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.hasExtra(EXTRA_INTENT)) {
			Intent launchIntent = intent.getParcelableExtra(EXTRA_INTENT);
			context.startActivity(launchIntent);
//			context.startActivity(new Intent(context, Main.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		} else {
			super.onReceive(context, intent);
		}
	}
	
    @Override
    public void onDisabled(Context context) {
        if (Log.DEBUG) {
            Log.debug("disabled");
        }
        RedditApp.destroy();
        Refresher.cancelRefresh(context);
    }
    
    @Override
    public void onEnabled(Context context) {
    	super.onEnabled(context);
    	Preferences.configureIfFirstLaunch();
    	Refresher.scheduleRefresh(context);
    }

    @Override
    public void onDeleted(Context context, int[] widgetIds) {
        RedditDatabase database = RedditApp.getDatabase();
        for (int i = 0; i < widgetIds.length; i++) {
            database.deleteWidget(widgetIds[i]);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager widgetManager, int[] widgetIds) {
        RemoteViews remote = new RemoteViews(context.getPackageName(), Preferences.theme().getMainLayoutResource());
        RedditDatabase database = RedditApp.getDatabase();
        for (int widgetId : widgetIds) {
            widgetManager.updateAppWidget(widgetId, remote);
            WidgetInfo info = database.fetchWidget(widgetId);
            if (info != null) {
                if (Log.DEBUG) {
                    Log.debug("subreddit [ " + widgetId + "]: " + info.subreddit);
                }
                updateWidget(context, widgetId);
            }
        }
    }

    public static void launchIntent(Context context, Intent launchIntent) {
    	Intent intent = new Intent(context, RedditWidgetProvider.class);
    	intent.putExtra(EXTRA_INTENT, launchIntent);
    	context.sendBroadcast(intent);
    }
    
    public static void updateWidget(Context context, int widgetId) {
        context.startService(WidgetService.intent(context, widgetId));
    }

    static void refreshWidget(Context context, int widgetId) {
        context.startService(WidgetService.refreshIntent(context, widgetId, 0));
    }

//    public static Intent makeUpdateIntent(Context context, int widgetId) {
//        Intent intent = PostService.intent(context);
//        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
//        // this Uri data is to make the PendingIntent unique, so it wont be
//        // updated by FLAG_UPDATE_CURRENT
//        // so if there are multiple widget instances they wont override each
//        // other
//        return intent;
//    }
}
