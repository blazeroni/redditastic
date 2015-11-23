package com.blazeroni.reddit.activity;

import android.app.Activity;
import android.os.Bundle;

import com.blazeroni.reddit.activity.LoginFragment.OnLoginActionListener;
import com.blazeroni.reddit.model.LoginResponse.LoginError;
import com.blazeroni.reddit.util.Fragments;
import com.blazeroni.reddit.widget.R;

public class LoginActivity extends Activity implements OnLoginActionListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
    }

    @Override
    public void onLoginSuccess() {
        finish();
    }

    @Override
    public void onLoginError(LoginError error) {
        String message;
        switch (error) {
            case MISSING_INFO:
            case WRONG_PASSWORD:
                message = "Check your username and password and try again.";
                break;
            case RATE_LIMIT:
            case UNKNOWN:
            default:
                message = "Unable to login at this time.  Please try again later.";
                break;
        }
        Fragments.showDialog(getFragmentManager(), new ErrorDialogFragment(this, "Login Failed", message));
    }
}
