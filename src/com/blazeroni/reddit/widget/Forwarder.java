package com.blazeroni.reddit.widget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class Forwarder extends Activity {
	public static final String EXTRA_INTENT = "com.blazeroni.reddit.widget.INTENT";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		if (intent.hasExtra(EXTRA_INTENT)) {
			Intent launchIntent = intent.getParcelableExtra(EXTRA_INTENT);
			startService(launchIntent);
		}
		
		finish();
	}
	
    public static Intent createForwardingIntent(Context context, Intent launchIntent) {
    	Intent intent = new Intent(context, Forwarder.class);
    	if (launchIntent != null) {
    		intent.setData(launchIntent.getData());
	    	intent.setAction(launchIntent.getAction()); // differentiate for PendingIntents
	    	intent.putExtra(EXTRA_INTENT, launchIntent);
    	}
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	return intent;
    }
}
