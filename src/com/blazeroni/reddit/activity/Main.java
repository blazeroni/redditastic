package com.blazeroni.reddit.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.blazeroni.reddit.util.Fragments;
import com.blazeroni.reddit.widget.R;

public class Main extends SettingsActivity {
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.help) {
            Fragments.showDialog(getFragmentManager(), new HelpDialogFragment());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class HelpDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            };

            return new AlertDialog.Builder(getActivity())
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setTitle(R.string.faq)
                    .setMessage(R.string.faq_message)
                    .setPositiveButton(R.string.close, listener)
                    .create();
        }
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, Main.class);
    }
}
