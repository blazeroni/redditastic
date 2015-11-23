package com.blazeroni.reddit.util;

import com.blazeroni.reddit.widget.R;

public class DarkTheme implements Theme {
    @Override
    public int getMainLayoutResource() {
        return R.layout.main_dark;
    }

    @Override
    public int getBackgroundColorResource() {
        return android.R.color.transparent;
    }

    @Override
    public int getTextColorResource() {
        return R.color.light_text;
    };

    @Override
    public int getSecondaryTextColorResource() {
        return R.color.light_secondary_text;
    }

    @Override
    public int getListLoadingViewResource() {
        return R.layout.loading_list_item_dark;
    }
}
