package com.blazeroni.reddit.util;

import android.app.FragmentManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.blazeroni.reddit.activity.ErrorDialogFragment;

public class Network {
    public static boolean isOnline(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();

        if (info != null) {
            return info.isConnected();
        }

        return false;
    }

    public static boolean isOnlineElseDialog(Context context, FragmentManager fragmentManager) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();

        if (info != null) {
            return info.isConnected();
        }

        Fragments.showDialog(fragmentManager, new ErrorDialogFragment(context, "Login Failed", "Unable to connect to the Internet. Check your connection and try again."));
        return false;
    }
}
