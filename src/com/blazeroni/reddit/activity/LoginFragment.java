package com.blazeroni.reddit.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.blazeroni.reddit.RedditApp;
import com.blazeroni.reddit.db.WidgetTable.WidgetColumns;
import com.blazeroni.reddit.http.Http;
import com.blazeroni.reddit.model.LoginResponse;
import com.blazeroni.reddit.model.LoginResponse.LoginError;
import com.blazeroni.reddit.model.Subreddits;
import com.blazeroni.reddit.model.User;
import com.blazeroni.reddit.util.Log;
import com.blazeroni.reddit.util.Network;
import com.blazeroni.reddit.util.Objects;
import com.blazeroni.reddit.util.Preferences;
import com.blazeroni.reddit.util.SafeAsyncTask;
import com.blazeroni.reddit.util.Strings;
import com.blazeroni.reddit.widget.R;
import com.blazeroni.reddit.widget.WidgetService;

public class LoginFragment extends Fragment implements OnClickListener, OnEditorActionListener {
    public interface OnLoginActionListener {
        void onLoginSuccess();
        void onLoginError(LoginError error);
    }

    private static final String[] PROJECTION = new String[] { WidgetColumns.WIDGET_ID + " AS " + BaseColumns._ID, WidgetColumns.SUBREDDIT };

    private OnLoginActionListener listener;

    private EditText username;
    private EditText password;
    private Button ok;

    private ViewGroup statusContainer;
    private TextView status;

    private Handler handler;

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.listener = Objects.ensureClass(activity, OnLoginActionListener.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.handler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_fragment, container, false);

        this.username = (EditText) view.findViewById(R.id.username);
        this.password = (EditText) view.findViewById(R.id.password);
        this.password.setOnEditorActionListener(this);

        // fix for monospace hint font
        this.password.setTypeface(Typeface.DEFAULT);

        this.ok = (Button) view.findViewById(R.id.ok);

        this.ok.setOnClickListener(this);

        this.statusContainer = (ViewGroup) view.findViewById(R.id.status_container);
        this.status = (TextView) view.findViewById(R.id.status);

        return view;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v.getId() == R.id.password && actionId == EditorInfo.IME_ACTION_GO) {
            login();
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ok:
                if (Network.isOnlineElseDialog(v.getContext(), getFragmentManager())) {
                    login();
                }
                break;
        }
    }

    private void loggingIn(boolean active) {
        this.username.setEnabled(!active);
        this.password.setEnabled(!active);

        this.ok.setEnabled(!active);

        int visibility = active ? VISIBLE : GONE;

        this.statusContainer.setVisibility(visibility);
    }

    private void login() {
        View view = getView();

        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromInputMethod(view.getWindowToken(), 0);

        final String username = this.username.getText().toString();
        final String password = this.password.getText().toString();

        if (Strings.isEmpty(username) || Strings.isEmpty(password)) {
            this.listener.onLoginError(LoginError.MISSING_INFO);
            return;
        }

        loggingIn(true);

        updateStatus("Logging in…");

        new SafeAsyncTask<LoginResponse>() {
            private LoginError error;

            @Override
            public LoginResponse call() throws Exception {
                InputStream stream = null;
                LoginResponse response = null;
                try {
                    stream = Http.login(username, password);
                    response = RedditApp.getMapper().readValue(stream, LoginResponse.class);
                    if (!response.isSuccess()) {
                        this.error = response.getError();
                        throw new RuntimeException("Login failed");
                    }
                } finally {
                    IOUtils.closeQuietly(stream);
                }

                // not ideal, but it's done once in uncommonly used code
                LoginFragment.this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateStatus("Retrieving subscribed subreddits…");
                    }
                });

                try {
                    stream = Http.fetchSubscribedSubreddits();
                    Subreddits subreddits = RedditApp.getMapper().readValue(stream, Subreddits.class);
                    Preferences.saveSubreddits(subreddits.getSubreddits());
                } finally {
                    IOUtils.closeQuietly(stream);
                }

                return response;
            }

            @Override
            protected void onSuccess(LoginResponse response) throws Exception {
                if (Log.DEBUG) {
                    Log.debug("Logging in as " + username);
                }
                User user = new User();
                user.setUsername(username);
                user.setModhash(response.getModhash());
                user.setCookie(response.getCookie());

                RedditApp.setUser(user);

                WidgetService.refreshFrontPageWidgets(getView().getContext());

                LoginFragment.this.listener.onLoginSuccess();
            }

            @Override
            protected void onException(Exception e) {
                Log.error("Failed logging in", e);
                RedditApp.setUser(null);
                LoginFragment.this.listener.onLoginError(this.error != null ? this.error : LoginError.UNKNOWN);
            }

            @Override
            protected void onFinally() throws RuntimeException {
                loggingIn(false);
            }
        }.execute();
    }

    private void updateStatus(String status) {
        this.status.setText(status);
    }
}
