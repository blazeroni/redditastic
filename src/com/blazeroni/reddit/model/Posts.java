package com.blazeroni.reddit.model;

import java.util.ArrayList;

import org.codehaus.jackson.map.annotate.JsonDeserialize;

import com.blazeroni.reddit.json.PostsDeserializer;

@JsonDeserialize(using=PostsDeserializer.class)
public class Posts {
    private ArrayList<Post> posts = new ArrayList<Post>();
    private String next;

    public void setPosts(ArrayList<Post> posts) {
        this.posts = posts;
    }

    public ArrayList<Post> getPosts() {
        return this.posts;
    }

    public boolean isEmpty() {
        return this.posts == null || this.posts.isEmpty();
    }

    public String getNext() {
        return this.next;
    }

    public void setNext(String next) {
        this.next = next;
    }
}
