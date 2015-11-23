package com.blazeroni.reddit.util;

import android.view.View;
import android.view.ViewGroup;

public class Views {
	@SuppressWarnings("unchecked")
	public static <T> T findViewByClass(View view, Class<T> clazz) {
		if (view instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) view;
			int count = group.getChildCount();
			for (int i = 0; i < count; i++) {
				view = group.getChildAt(i);
				T result = findViewByClass(view, clazz);
				if (result != null) {
					return result;
				}
			}
			return null;
		} else if (clazz.isInstance(view)) {
			return (T) view;
		} else {
			return null;
		}
	}
}
