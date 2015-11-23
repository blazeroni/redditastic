package com.blazeroni.reddit.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.blazeroni.reddit.util.Log;
import com.blazeroni.reddit.util.Preferences;
import com.blazeroni.reddit.util.Refresher;

public class UpgradeReceiver extends BroadcastReceiver {
	public void onReceive(Context context, Intent intent) {
		Uri uri = intent.getData();
		if (uri != null && context.getPackageName().equals(uri.getSchemeSpecificPart())) {
			Log.info("Upgrading app");
			Preferences.configureIfFirstLaunch();
			Refresher.scheduleRefresh(context);
		}
	}
}
