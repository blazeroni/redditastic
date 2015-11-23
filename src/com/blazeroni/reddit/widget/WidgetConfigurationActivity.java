package com.blazeroni.reddit.widget;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils.SimpleStringSplitter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.blazeroni.reddit.RedditApp;
import com.blazeroni.reddit.activity.ErrorDialogFragment;
import com.blazeroni.reddit.activity.SettingsActivity;
import com.blazeroni.reddit.model.PostSort;
import com.blazeroni.reddit.model.WidgetInfo;
import com.blazeroni.reddit.util.Fragments;
import com.blazeroni.reddit.util.Log;
import com.blazeroni.reddit.util.Strings;
import com.blazeroni.reddit.util.Subreddits;

public class WidgetConfigurationActivity extends Activity {
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private Pattern PATTERN = Pattern.compile("^\\s*/?(?:r/)?(\\w+)\\b.*");

    private LayoutInflater inflater;

    private ViewGroup container;
    private Button frontPage;
    private Button addSubreddit;
    private Spinner sortPicker;
    private EditText nameEdit;

    List<String> subreddits = Subreddits.subreddits();

    ArrayList<SubredditViews> views = new ArrayList<SubredditViews>();

    private OnItemSelectedListener spinnerListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            updateDefaultName();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    private TextWatcher subredditEditWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateDefaultName();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configure);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        this.widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

        if (this.widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        final WidgetInfo info = RedditApp.getDatabase().fetchWidget(this.widgetId);

        this.inflater = LayoutInflater.from(this);
        this.container = (ViewGroup) findViewById(R.id.subreddits);

        final View detailContainer = findViewById(R.id.detail_container);

        this.addSubreddit = (Button) findViewById(R.id.add_another);
        final Button customize = (Button) findViewById(R.id.customize);
        customize.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                WidgetConfigurationActivity.this.container.removeAllViews();
                detailContainer.setVisibility(VISIBLE);
                WidgetConfigurationActivity.this.addSubreddit.setVisibility(VISIBLE);
                addSubredditSelector();
                updateDefaultName();
            }
        });

        final View or = findViewById(R.id.or);
        this.frontPage = (Button) findViewById(R.id.front_page);
        this.frontPage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                customize.setVisibility(View.GONE);
                or.setVisibility(View.GONE);
                WidgetConfigurationActivity.this.frontPage.setBackgroundDrawable(null);
                WidgetConfigurationActivity.this.frontPage.setClickable(false);
                detailContainer.setVisibility(VISIBLE);
                WidgetConfigurationActivity.this.nameEdit.setHint(R.string.front_page_default_name);
            }
        });

        this.nameEdit = (EditText) findViewById(R.id.name);
        if (info != null) {
            this.nameEdit.setText(info.name);
        }

        this.sortPicker = (Spinner) findViewById(R.id.sort_picker);
        this.sortPicker.setAdapter(new SortAdapter());

        this.addSubreddit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addSubredditSelector();
            }
        });

        Button submit = (Button) findViewById(R.id.submit);
        submit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updateWidget(info != null);
            }
        });

        if (info != null) {
            if (info.subreddit.isEmpty()) {
                this.frontPage.performClick();
            } else {
                // refactor - same as above
                this.container.removeAllViews();
                detailContainer.setVisibility(VISIBLE);
                this.addSubreddit.setVisibility(VISIBLE);

                SimpleStringSplitter splitter = new SimpleStringSplitter('+');
                splitter.setString(info.subreddit.substring(3));
                for (String s : splitter){
                    addSubredditSelector(s);
                }
            }

            this.sortPicker.setSelection(info.sort - 1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.widget_menu, menu);
        return true;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        List<String> list = Subreddits.subreddits();
        if (!this.subreddits.equals(list)) {
            int size = this.views.size();
            String[] current = new String[size];

            // find out what the spinners value is right now
            for (int i = 0; i < size; i++) {
                SubredditViews sv = this.views.get(i);
                SubredditAdapter adapter = (SubredditAdapter) sv.spinner.getAdapter();
                if (sv.spinner.getVisibility() == VISIBLE) {
                    int position = sv.spinner.getSelectedItemPosition();
                    current[i] = position >= 0 ? adapter.getItem(position) : null;
                } else {
                    current[i] = sv.editText.getText().toString();
                }
            }

            this.subreddits = list;

            // find out where that new value is
            for (int i = 0; i < size; i++) {
                SubredditViews sv = this.views.get(i);
                String value = current[i];
                if (Strings.notEmpty(value)) {
                    int position = Collections.binarySearch(list, value, String.CASE_INSENSITIVE_ORDER);
                    if (position >= 0) {
                        convertEditToSpinner(sv);
                        sv.spinner.setSelection(position);
                        ((SubredditAdapter) sv.spinner.getAdapter()).notifyDataSetChanged();
                    } else {
                        convertSpinnerToEdit(sv);
                        sv.editText.setText(value);
                    }
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(SettingsActivity.createIntent(this));
                return true;
        }
        return false;
    }

    private void updateWidget(boolean edit) {
        Context context = WidgetConfigurationActivity.this;

        final String value;
        boolean valid = false;
        if (this.frontPage.isShown()) {
            value = "";
            valid = true;
        } else {
            value = computeSubreddit();
            if (!value.equals("/r/")) {
                valid = true;
            }
        }

        if (!valid) {
            Fragments.showDialog(getFragmentManager(), new ErrorDialogFragment(this, R.string.error_title_no_subreddit, R.string.error_msg_no_subreddit));
            return;
        }

        if (Log.DEBUG) {
            Log.debug("subreddit: " + value);
        }

        String userName = this.nameEdit.getText().toString();
        String name = !userName.isEmpty() ? userName : null;
        PostSort sort = (PostSort) this.sortPicker.getSelectedItem();

        RedditApp.getDatabase().save(this.widgetId, name, value, sort);

        if (edit) {
            RedditWidgetProvider.refreshWidget(context, this.widgetId);
        } else {
            RedditWidgetProvider.updateWidget(context, this.widgetId);
        }

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this.widgetId);

        setResult(RESULT_OK, result);
        finish();
    }

    private String normalizeSubredditPrefix(String value) {
        Matcher matcher = this.PATTERN.matcher(value);
        if (matcher.matches()) {
            value = matcher.group(1);
        } else {
            value = null;
        }
        return value;
    }

    private void addSubredditSelector(String value) {
        final View selector = this.inflater.inflate(R.layout.configure_subreddit, this.container, false);

        final Spinner subredditPicker = (Spinner) selector.findViewById(R.id.subreddit_picker);
        subredditPicker.setAdapter(new SubredditAdapter());
        subredditPicker.setOnItemSelectedListener(this.spinnerListener);

        final EditText subredditEdit = (EditText) selector.findViewById(R.id.subreddit_edit);
        subredditEdit.addTextChangedListener(this.subredditEditWatcher);

        final ImageButton editSubreddit = (ImageButton) selector.findViewById(R.id.edit_subreddit);

        final SubredditViews sv = new SubredditViews(subredditPicker, subredditEdit, editSubreddit);
        this.views.add(sv);

        editSubreddit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                convertSpinnerToEdit(sv);
                sv.editText.requestFocus();

                long millis = SystemClock.uptimeMillis();
                subredditEdit.dispatchTouchEvent(MotionEvent.obtain(millis, millis, MotionEvent.ACTION_DOWN, 0, 0, 0));
                subredditEdit.dispatchTouchEvent(MotionEvent.obtain(millis, millis, MotionEvent.ACTION_UP, 0, 0, 0));

                updateDefaultName();
            }
        });

        ImageButton removeSubreddit = (ImageButton) selector.findViewById(R.id.remove_subreddit);
        removeSubreddit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                WidgetConfigurationActivity.this.views.remove(sv);
                WidgetConfigurationActivity.this.container.removeView(selector);
                updateDefaultName();
            }
        });

        if (value != null) {
            List<String> subreddits = Subreddits.subreddits();
            int index = Collections.binarySearch(subreddits, value, String.CASE_INSENSITIVE_ORDER);
            if (index >= 0) {
                subredditPicker.setSelection(index);
            } else {
                convertSpinnerToEdit(sv);
                subredditEdit.setText(value);
            }
        }

        this.container.addView(selector);
//        this.numSubreddits++;
    }

    private void updateDefaultName() {
        this.nameEdit.setHint(Subreddits.defaultName(computeSubreddit()));
    }

    private void convertSpinnerToEdit(SubredditViews sv) {
        sv.spinner.setVisibility(GONE);
        sv.editButton.setVisibility(GONE);
        sv.editText.setVisibility(VISIBLE);
    }

    private void convertEditToSpinner(SubredditViews sv) {
        sv.spinner.setVisibility(VISIBLE);
        sv.editButton.setVisibility(VISIBLE);
        sv.editText.setVisibility(GONE);
    }

    private String computeSubreddit() {
        int size = this.views.size();
        ArrayList<String> subreddits = new ArrayList<String>(size);

        for (int i = 0; i < size; i++) {
        SubredditViews sv = this.views.get(i);
            Spinner spinner = sv.spinner;
            if (spinner.isShown()) {
                String sr = (String) spinner.getSelectedItem();
                if (sr != null && !sr.isEmpty()) {
                    subreddits.add(sr);
                }
            } else {
                EditText editText = sv.editText;
                String subreddit = normalizeSubredditPrefix(editText.getText().toString());
                if (subreddit != null) {
                    subreddits.add(subreddit);
                }
            }
        }

        return "/r/" + StringUtils.join(subreddits, "+");
    }

    private void addSubredditSelector() {
        addSubredditSelector(null);
    }

    /*package*/ class SubredditAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return WidgetConfigurationActivity.this.subreddits.size();
        }

        @Override
        public String getItem(int position) {
            return WidgetConfigurationActivity.this.subreddits.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) convertView;
            if (view == null) {
                view = (TextView) WidgetConfigurationActivity.this.inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
                view.setHint(R.string.subreddit);
            }
            if (position >= 0) {
                view.setText(WidgetConfigurationActivity.this.subreddits.get(position));
            }
            return view;
        }
    }

    /*package*/ class SortAdapter extends BaseAdapter {
        PostSort[] sorts = PostSort.values();

        @Override
        public int getCount() {
            return this.sorts.length;
        }

        @Override
        public PostSort getItem(int position) {
            return this.sorts[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) convertView;
            if (view == null) {
                view = (TextView) WidgetConfigurationActivity.this.inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
            }
            view.setText(this.sorts[position].getStringId());
            return view;
        }
    }

    private class SubredditViews {
        final Spinner spinner;
        final EditText editText;
        final ImageButton editButton;

        SubredditViews(Spinner spinner, EditText editText, ImageButton editButton) {
            this.spinner = spinner;
            this.editText = editText;
            this.editButton = editButton;
        }
    }
}
