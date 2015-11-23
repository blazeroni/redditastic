package com.blazeroni.reddit.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

public class ErrorDialogFragment extends DialogFragment {
    private final Context context;
    private final String title;
    private final String message;

    public ErrorDialogFragment(Context context, String title, String message) {
        this.context = context;
        this.title = title;
        this.message = message;
    }

    public ErrorDialogFragment(Context context, int titleRes, int messageRes) {
        this.context = context;
        this.title = context.getString(titleRes);
        this.message = context.getString(messageRes);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(this.context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(this.title)
                .setMessage(this.message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }).create();
    }
}
