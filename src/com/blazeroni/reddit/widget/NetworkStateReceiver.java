package com.blazeroni.reddit.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.blazeroni.reddit.util.Log;
import com.blazeroni.reddit.util.Network;
import com.blazeroni.reddit.util.Preferences;
import com.commonsware.cwac.wakeful.WakefulIntentService;

public class NetworkStateReceiver extends BroadcastReceiver {
	public void onReceive(Context context, Intent intent) {
		Log.debug("Network connectivity change");

		if (Preferences.refreshNeeded() && Network.isOnline(context)) {
			WakefulIntentService.sendWakefulWork(context, RefreshService.createIntent(context));
			Preferences.saveRefreshNeeded(false);
		}
	}
}
