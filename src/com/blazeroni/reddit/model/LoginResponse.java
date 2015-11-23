package com.blazeroni.reddit.model;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.annotate.JsonDeserialize;

import com.blazeroni.reddit.json.LoginResponseDeserializer;
import com.blazeroni.reddit.util.Log;

@JsonDeserialize(using=LoginResponseDeserializer.class)
public class LoginResponse {
    public enum LoginError {
        MISSING_INFO(null),
        WRONG_PASSWORD("WRONG_PASSWORD"),
        RATE_LIMIT("RATELIMIT"),
        UNKNOWN(null);

        private final String key;

        LoginError(String key) {
            this.key = key;
        }

        public static LoginError fromKey(String key) {
            if (key == null) {
                return UNKNOWN;
            }
            for (LoginError error : values()) {
                if (key.equals(error.key)) {
                    return error;
                }
            }
            if (Log.DEBUG) {
                Log.debug("Unknown login error key: " + key);
            }
            return UNKNOWN;
        }
    }

    private List<LoginError> errors;
    private String modhash;
    private String cookie;

    public boolean isSuccess() {
        return this.errors == null && this.modhash != null && this.cookie != null;
    }

    public String getModhash() {
        return this.modhash;
    }
    public void setModhash(String modhash) {
        this.modhash = modhash;
    }
    public String getCookie() {
        return this.cookie;
    }
    public void setCookie(String cookie) {
        this.cookie = cookie;
    }
    public LoginError getError() {
        if (isSuccess()) {
            return null;
        }
        if (this.errors != null && !this.errors.isEmpty()) {
            return this.errors.get(0);
        }
        return LoginError.UNKNOWN;
    }

    public void addError(String key) {
        if (this.errors == null) {
            this.errors = new ArrayList<LoginError>();
        }
        this.errors.add(LoginError.fromKey(key));
    }

}
