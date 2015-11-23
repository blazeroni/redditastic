package com.blazeroni.reddit.activity;

import static com.blazeroni.reddit.util.Preferences.ACCOUNT;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.BaseColumns;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.TextView;

import com.blazeroni.reddit.RedditApp;
import com.blazeroni.reddit.db.WidgetTable.WidgetColumns;
import com.blazeroni.reddit.http.Http;
import com.blazeroni.reddit.model.Subreddits;
import com.blazeroni.reddit.model.User;
import com.blazeroni.reddit.util.Device;
import com.blazeroni.reddit.util.Fragments;
import com.blazeroni.reddit.util.Network;
import com.blazeroni.reddit.util.Preferences;
import com.blazeroni.reddit.util.Refresher;
import com.blazeroni.reddit.util.SafeAsyncTask;
import com.blazeroni.reddit.widget.R;
import com.blazeroni.reddit.widget.WidgetService;

public class SettingsFragment extends PreferenceFragment implements OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String[] PROJECTION = new String[] { WidgetColumns.WIDGET_ID + " AS " + BaseColumns._ID };
    private static final String REFRESH = "refresh";

    private Preference twoState;
    private Preference hcFrequency;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Preferences.configureIfFirstLaunch();
        
        addPreferencesFromResource(R.xml.account_preferences);
        addPreferencesFromResource(R.xml.widget_preferences);

        PreferenceManager preferenceManager = getPreferenceManager();

        this.twoState = preferenceManager.findPreference(Preferences.AUTO_REFRESH_ENABLED);
        this.twoState.setOnPreferenceChangeListener(this);
        
        if (Device.isIceCreamSancwichPlus()) {
        	this.twoState.setOnPreferenceClickListener(this);	
        } else {
        	hcFrequency = preferenceManager.findPreference(Preferences.REFRESH_FREQUENCY);
        	hcFrequency.setOnPreferenceClickListener(this);
        	hcFrequency.setOnPreferenceChangeListener(this);
        }
        
        updateRefreshFrequencyPreference(getActivity(), Preferences.autoRefreshEnabled());

        ListPreference preference = (ListPreference) preferenceManager.findPreference(Preferences.WIDGET_CLICK_ACTION);
        int value;
        try {
            value = Integer.parseInt(preference.getValue());
        } catch (Exception e) {
            preference.setValue(Preferences.DEFAULT_CLICK_ACTION);
            value = Integer.parseInt(Preferences.DEFAULT_CLICK_ACTION);
        }
        preference.setOnPreferenceChangeListener(this);
        updateClickActionPreference(preference, value);

        preference = (ListPreference) preferenceManager.findPreference(Preferences.THEME);
        try {
            value = Integer.parseInt(preference.getValue());
        } catch (Exception e) {
            preference.setValue(Preferences.DEFAULT_THEME);
            value = Integer.parseInt(Preferences.DEFAULT_THEME);
        }
        preference.setOnPreferenceChangeListener(this);
        updateThemePreference(preference, value);

        preference = (ListPreference) preferenceManager.findPreference(Preferences.ACCENT_COLOR);
        try {
            value = Integer.parseInt(preference.getValue());
        } catch (Exception e) {
            preference.setValue(Preferences.DEFAULT_ACCENT_COLOR);
            value = Integer.parseInt(Preferences.DEFAULT_ACCENT_COLOR);
        }
        preference.setOnPreferenceChangeListener(this);
        updateAccentColorPreference(preference, value);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateUserPreference();
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        boolean loggedIn = RedditApp.getUser().isLoggedIn();
        if (loggedIn && ACCOUNT.equals(preference.getKey())) {
            Fragments.showDialog(getFragmentManager(), new LogoutDialogFragment());
            return true;
        } else if (loggedIn && REFRESH.equals(preference.getKey()) && Network.isOnlineElseDialog(preference.getContext(), getFragmentManager())) {
            preference.setWidgetLayoutResource(R.layout.progress_bar);
            preference.setSummary(R.string.refreshing);
            preference.setEnabled(false);
            new SafeAsyncTask<Void>() {
                @Override
                public Void call() throws Exception {
                    InputStream stream = null;
                    try {
                        stream = Http.fetchSubscribedSubreddits();
                        Subreddits subreddits = RedditApp.getMapper().readValue(stream, Subreddits.class);
                        Preferences.saveSubreddits(subreddits.getSubreddits());
                    } finally {
                        IOUtils.closeQuietly(stream);
                    }
                    return null;
                }

                @Override
                protected void onException(Exception e) throws RuntimeException {

                }

                @Override
                protected void onSuccess(Void t) throws Exception {
                    preference.setWidgetLayoutResource(0);
                    preference.setSummary(R.string.last_updated_now);
                    preference.setEnabled(true);
                }
            }.execute();
            return true;
        } else if (Device.isIceCreamSancwichPlus() ? Preferences.AUTO_REFRESH_ENABLED.equals(preference.getKey()) : Preferences.REFRESH_FREQUENCY.equals(preference.getKey())) {
            Fragments.showDialog(getFragmentManager(), new RefreshFrequencyDialogFragment());
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (key.equals(Preferences.WIDGET_CLICK_ACTION)) {
        	int clickAction = Integer.parseInt(newValue.toString());
        	Preferences.saveClickAction(clickAction);
            updateClickActionPreference((ListPreference) preference, clickAction);
            Cursor cursor = RedditApp.getDatabase().fetchAllWidgets(PROJECTION);
            try {
                Context context = getView().getContext();
                while (cursor.moveToNext()) {
                    int widgetId = cursor.getInt(0);
                    context.startService(WidgetService.updateViewIntent(context, widgetId));
                }
            } finally {
                cursor.close();
            }
            return true;
        } else if (key.equals(Preferences.THEME)) {
            updateThemePreference((ListPreference) preference, Integer.parseInt(newValue.toString()));
            Cursor cursor = RedditApp.getDatabase().fetchAllWidgets(PROJECTION);
            try {
                Context context = getView().getContext();
                AppWidgetManager manager = AppWidgetManager.getInstance(context);
                while (cursor.moveToNext()) {
                    int widgetId = cursor.getInt(0);
                    context.startService(WidgetService.updateViewIntent(context, widgetId));
                    manager.notifyAppWidgetViewDataChanged(widgetId, R.id.list);
                }
            } finally {
                cursor.close();
            }
            return true;
        } else if (key.equals(Preferences.ACCENT_COLOR)) {
            updateAccentColorPreference((ListPreference) preference, Integer.parseInt(newValue.toString()));
            Cursor cursor = RedditApp.getDatabase().fetchAllWidgets(PROJECTION);
            try {
                Context context = getView().getContext();
                while (cursor.moveToNext()) {
                    int widgetId = cursor.getInt(0);
                    context.startService(WidgetService.updateViewIntent(context, widgetId));
                }
            } finally {
                cursor.close();
            }
            return true;
        } else if (Preferences.AUTO_REFRESH_ENABLED.equals(preference.getKey())) {
        	Preferences.saveAutoRefreshEnabled((Boolean) newValue);
            updateRefreshFrequencyPreference(preference.getContext(), (Boolean) newValue);
            Refresher.scheduleRefresh(getView().getContext());
            return true;
        } else if (!Device.isIceCreamSancwichPlus() && Preferences.REFRESH_FREQUENCY.equals(preference.getKey())) {
        	Refresher.scheduleRefresh(getView().getContext());
        }
        return false;
    }

    private void updateUserPreference() {
        PreferenceManager preferenceManager = getPreferenceManager();
        User user = RedditApp.getUser();

        boolean loggedIn = user.isLoggedIn();
        Preference accountPreference = preferenceManager.findPreference(Preferences.ACCOUNT);
        Preference refreshPreference = preferenceManager.findPreference(REFRESH);
        if (loggedIn) {
            accountPreference.setSummary(user.getUsername());
            accountPreference.setOnPreferenceClickListener(this);
            refreshPreference.setOnPreferenceClickListener(this);

            long time = Preferences.lastSubredditRefresh();
            if (time != 0) {
                long current = System.currentTimeMillis();
                if (current - time < 5000) {
                    refreshPreference.setSummary(getString(R.string.last_updated_now));
                } else {
                    String s = DateUtils.getRelativeDateTimeString(getActivity(), time, DateUtils.SECOND_IN_MILLIS, DateUtils.YEAR_IN_MILLIS, 0).toString();
                    refreshPreference.setSummary(getString(R.string.last_updated_f, s));
                }
            } else {
                refreshPreference.setSummary(null);
            }
        } else {
            accountPreference.setSummary(R.string.not_signed_in);
            refreshPreference.setSummary(null);
        }
        refreshPreference.setEnabled(loggedIn);
    }

    private void updateClickActionPreference(ListPreference preference, int value) {
        Resources r = preference.getContext().getResources();
        String summary = null;
        switch (value) {
            case Preferences.CLICK_ACTION_VIEW_COMMENTS:
                summary = r.getString(R.string.view_comments);
                break;
            case Preferences.CLICK_ACTION_VIEW_LINK:
                summary = r.getString(R.string.view_link);
                break;
            case Preferences.CLICK_ACTION_PROMPT:
                summary = r.getString(R.string.ask_me);
                break;
        }
        preference.setSummary(summary);
    }

    private void updateThemePreference(ListPreference preference, int value) {
        Resources r = preference.getContext().getResources();
        String summary = null;
        switch (value) {
            case Preferences.THEME_LIGHT:
                summary = r.getString(R.string.light);
                break;
            case Preferences.THEME_DARK:
                summary = r.getString(R.string.dark);
                break;
        }
        preference.setSummary(summary);
    }

    private void updateAccentColorPreference(ListPreference preference, int value) {
        Resources r = preference.getContext().getResources();
        String summary = null;
        switch (value) {
            case Preferences.ACCENT_COLOR_ICS_BLUE:
                summary = r.getString(R.string.ics_blue);
                break;
            case Preferences.ACCENT_COLOR_ORANGERED:
                summary = r.getString(R.string.orangered);
                break;
            case Preferences.ACCENT_COLOR_WHITE:
                summary = r.getString(R.string.white);
                break;
            case Preferences.ACCENT_COLOR_TRANSPARENT:
                summary = r.getString(R.string.transparent);
                break;
        }
        preference.setSummary(summary);
    }

    private void updateRefreshFrequencyPreference(Context context, boolean value) {
        Resources r = context.getResources();
        int frequency = Preferences.refreshFrequency();
        if (Device.isIceCreamSancwichPlus()) {
        	twoState.setSummary(value ? r.getQuantityString(R.plurals.every_x_hours, frequency, frequency) : r.getString(R.string.disabled));
        } else {
        	hcFrequency.setSummary(r.getQuantityString(R.plurals.every_x_hours, frequency, frequency));
        }
    }

    private void logout() {
        RedditApp.setUser(null);
        updateUserPreference();
        WidgetService.refreshFrontPageWidgets(getView().getContext());
    }

    public class LogoutDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            logout();
                            dialog.dismiss();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            dialog.dismiss();
                            break;
                    }
                }
            };

            return new AlertDialog.Builder(getActivity())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.logout)
                    .setMessage(R.string.logout_message)
                    .setPositiveButton(R.string.logout, listener)
                    .setNegativeButton(android.R.string.cancel, listener)
                    .create();
        }
    }

    public class RefreshFrequencyDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            LayoutInflater inflater = LayoutInflater.from(context);
            final View view = inflater.inflate(R.layout.hour_picker, null, false);

            final Resources r = context.getResources();
            final TextView hours = (TextView) view.findViewById(R.id.hours);

            int current = Preferences.refreshFrequency();

            hours.setText(r.getQuantityString(R.plurals.every_x_hours_post, current != -1 ? current : 1));

            final NumberPicker picker = (NumberPicker) view.findViewById(R.id.number_picker);
            picker.setMinValue(1);
            picker.setMaxValue(24);
            picker.setWrapSelectorWheel(false);
            picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
            picker.setValue(current != -1 ? current : 1);
            picker.setOnValueChangedListener(new OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    if (oldVal != newVal) {
                        hours.setText(r.getQuantityString(R.plurals.every_x_hours_post, newVal));
                    }
                }
            });

            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            Preferences.saveRefreshFrequency(picker.getValue());
                            setChecked(SettingsFragment.this.twoState);
                            updateRefreshFrequencyPreference(context, true);
                            Refresher.scheduleRefresh(context);
                            dialog.dismiss();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            dialog.dismiss();
                            break;
                    }
                }
            };

            return new AlertDialog.Builder(context)
                    .setView(view)
                    .setTitle(R.string.auto_refresh_frequency)
                    .setPositiveButton(R.string.set, listener)
                    .setNegativeButton(android.R.string.cancel, listener)
                    .create();
        }

        @TargetApi(14)
        void setChecked(Preference preference) {
            if (preference instanceof CheckBoxPreference) {
                // pre-API 14, CheckBoxPreference doesn't extend from TwoStatePreference
                ((CheckBoxPreference) preference).setChecked(true);
            } else {
                ((SwitchPreference) preference).setChecked(true);
            }
        }
    }
}