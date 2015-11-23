package com.blazeroni.reddit;

import org.codehaus.jackson.map.ObjectMapper;

import android.app.Application;
import android.content.Context;

import com.blazeroni.reddit.db.RedditDatabase;
import com.blazeroni.reddit.http.Http;
import com.blazeroni.reddit.model.User;
import com.blazeroni.reddit.util.ImageLoader;
import com.blazeroni.reddit.util.Log;
import com.blazeroni.reddit.util.Preferences;

public class RedditApp extends Application {
    private RedditDatabase database;
    private ImageLoader imageLoader;
    private ObjectMapper mapper;
    private User user;

    private static RedditApp INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
    }

    public static Context getContext() {
        return INSTANCE;
    }

    public synchronized static RedditDatabase getDatabase() {
        if (INSTANCE.database == null) {
            INSTANCE.database = new RedditDatabase(INSTANCE);
        }
        return INSTANCE.database;
    }

    public synchronized static ImageLoader getImageLoader() {
        if (INSTANCE.imageLoader == null) {
            if (Log.DEBUG) {
                Log.debug("creating new ImageLoader");
            }
            INSTANCE.imageLoader = new ImageLoader(INSTANCE);
        }
        return INSTANCE.imageLoader;
    }

    public synchronized static ObjectMapper getMapper() {
        if (INSTANCE.mapper == null) {
            INSTANCE.mapper = new ObjectMapper();
        }
        return INSTANCE.mapper;
    }

    public synchronized static void close() {
        if (Log.DEBUG) {
            Log.debug("closing database");
        }
        if (INSTANCE.database != null) {
            INSTANCE.database.close();
            INSTANCE.database = null;
        }
    }

    public synchronized static void setUser(User user) {
        if (user == null) {
            Preferences.deleteUser();
            Http.deleteCookies();
            INSTANCE.user = null;
        } else {
            Preferences.saveUser(user);
            INSTANCE.user = user;
        }
    }

    public synchronized static User getUser() {
        if (INSTANCE.user == null) {
            INSTANCE.user = Preferences.loadUser();
            if (INSTANCE.user == null) {
                INSTANCE.user = new User();
            }
        }
        return INSTANCE.user;
    }

    public synchronized static void destroy() {
        RedditApp.close();
        INSTANCE.deleteDatabase(RedditDatabase.DATABASE_NAME);
        INSTANCE.database = null;
        INSTANCE.imageLoader = null;
        INSTANCE.mapper = null;
        INSTANCE.user = null;
    }
}
