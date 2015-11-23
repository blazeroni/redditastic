package com.blazeroni.reddit.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.blazeroni.reddit.widget.R;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }
}
