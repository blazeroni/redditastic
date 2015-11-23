package com.blazeroni.reddit.preference;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

public class DetailSwitchPreference extends SwitchPreference {

    public DetailSwitchPreference(Context context) {
        super(context);
    }

    public DetailSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DetailSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onClick() {
    };
}
