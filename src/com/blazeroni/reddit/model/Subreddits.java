package com.blazeroni.reddit.model;

import java.util.List;

import org.codehaus.jackson.map.annotate.JsonDeserialize;

import com.blazeroni.reddit.json.SubredditsDeserializer;

@JsonDeserialize(using=SubredditsDeserializer.class)
public class Subreddits {
    private List<String> subreddits;

    public void setSubreddits(List<String> subreddits) {
        this.subreddits = subreddits;
    }

    public List<String> getSubreddits() {
        return this.subreddits;
    }
}
