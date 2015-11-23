package com.blazeroni.reddit.widget;

import java.util.List;

import android.content.Context;
import android.content.Intent;

import com.blazeroni.reddit.RedditApp;
import com.blazeroni.reddit.model.WidgetInfo;
import com.blazeroni.reddit.util.Log;
import com.blazeroni.reddit.util.Network;
import com.blazeroni.reddit.util.Refresher;
import com.commonsware.cwac.wakeful.WakefulIntentService;

public class RefreshService extends WakefulIntentService {
	public RefreshService() {
		super("RefreshService");
	}
	
	@Override
	protected void doWakefulWork(Intent intent) {
		if (Network.isOnline(this)) {
			Log.debug("Refreshing widgets");
			Refresher.disableNetworkReceiver();
			List<WidgetInfo> widgets = RedditApp.getDatabase().fetchAllWidgets();
			for (WidgetInfo info : widgets) {
				WakefulIntentService.sendWakefulWork(this, PostService.createAutoRefreshIntent(this, info));
			}
		} else {
			Refresher.enableNetworkReceiver();
		}
	}
	
	public static Intent createIntent(Context context) {
		return new Intent(context, RefreshService.class);
	}
}
