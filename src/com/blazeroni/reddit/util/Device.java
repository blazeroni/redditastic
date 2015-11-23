package com.blazeroni.reddit.util;

import android.os.Build;

public class Device {
	public static boolean isIceCreamSancwichPlus() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	}
	
	public static boolean isJellyBeanPlus() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
	}
}
