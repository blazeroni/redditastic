package com.blazeroni.reddit.util;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

public class Fragments {
    private static final String DIALOG_TAG = "dialog";

    public static void showDialog(FragmentManager manager, DialogFragment fragment) {
        FragmentTransaction ft = manager.beginTransaction();
        Fragment prev = manager.findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        fragment.show(ft, DIALOG_TAG);
    }
}
