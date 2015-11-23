package com.blazeroni.reddit.preference;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

public class UpdatablePreference extends Preference {
    public UpdatablePreference(Context context) {
        super(context);
    }
    public UpdatablePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public UpdatablePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setWidgetLayoutResource(int widgetLayoutResId) {
        int original = getWidgetLayoutResource();
        super.setWidgetLayoutResource(widgetLayoutResId);
        if (original != widgetLayoutResId) {
            notifyChanged();
        }
    }
}
