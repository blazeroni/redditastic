package com.blazeroni.reddit.model;

import java.io.Serializable;

public class Subreddit implements Serializable {
    private static final String BASE_PATH = "/r/";

    private String name;
    private String path;

    public Subreddit() {
        // empty
    }

    public Subreddit(String name) {
        this.name = name;
        setPath(name);
    }

    public Subreddit(String name, String path) {
        this.name = name;
        setPath(path);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path.startsWith(BASE_PATH) ? path : BASE_PATH + path;
    }

    public String getPath() {
        return this.path;
    }
}
