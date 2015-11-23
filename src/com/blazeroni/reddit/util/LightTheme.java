package com.blazeroni.reddit.util;

import com.blazeroni.reddit.widget.R;

public class LightTheme implements Theme {
    @Override
    public int getMainLayoutResource() {
        return R.layout.main_light;
    }

    @Override
    public int getBackgroundColorResource() {
        return R.color.light_background;
    }

    @Override
    public int getTextColorResource() {
        return R.color.dark_text;
    }

    @Override
    public int getSecondaryTextColorResource() {
        return R.color.secondary_text;
    }

    @Override
    public int getListLoadingViewResource() {
        return R.layout.loading_list_item_light;
    }
}
