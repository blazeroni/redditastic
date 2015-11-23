package com.blazeroni.reddit.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.SystemClock;

import com.blazeroni.reddit.RedditApp;
import com.blazeroni.reddit.widget.NetworkStateReceiver;
import com.blazeroni.reddit.widget.RefreshService;

public class Refresher {
	private static final int HOURS_TO_MILLIS = 3600000;
//	private static final int HOURS_TO_MILLIS = 15000;
	
	public static void scheduleRefresh(Context context) {
		// cancel current alarms
		cancelRefresh(context);
		
		if (Preferences.autoRefreshEnabled()) {
			AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			
			long time = SystemClock.elapsedRealtime();
			long interval = Preferences.refreshFrequency() * HOURS_TO_MILLIS;
			long trigger = time + interval;
			if (Log.DEBUG) Log.debug("Scheduling alarm to refresh every " + interval / HOURS_TO_MILLIS + " hours");
			manager.setRepeating(AlarmManager.ELAPSED_REALTIME, trigger, interval, createIntent(context));
		}
	}
	
	public static void cancelRefresh(Context context) {
		if (Log.DEBUG) Log.debug("Cancelling alarms");
		AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		manager.cancel(createIntent(context));
	}
	
	private static PendingIntent createIntent(Context context) {
		return PendingIntent.getService(context, 0, RefreshService.createIntent(context), 0);
	}

	public static void enableNetworkReceiver() {
		if (Log.DEBUG) Log.debug("Enabling network receiver");
		Preferences.saveRefreshNeeded(true);
		updateNetworkReceiver(true);
	}

	public static void disableNetworkReceiver() {
		if (Log.DEBUG) Log.debug("Disabling network receiver");
		Preferences.saveRefreshNeeded(false);
		updateNetworkReceiver(false);
	}
	
	private static void updateNetworkReceiver(boolean enabled) {
		Context context = RedditApp.getContext();
		int flag = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
		ComponentName component=new ComponentName(context, NetworkStateReceiver.class);

		context.getPackageManager().setComponentEnabledSetting(component, flag, PackageManager.DONT_KILL_APP);
	}
}
