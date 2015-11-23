package com.blazeroni.reddit.model;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

import com.blazeroni.reddit.json.PostDeserializer;

@JsonDeserialize(using=PostDeserializer.class)
public class Post {
    private String id;
    private String fullId;
    private String title;
    private String subreddit;
    private String domain;
    private boolean selfPost;
    private String selfText;
//    @JsonProperty("ups") private int upVotes;
//    @JsonProperty("downs") private int downVotes;
    @JsonProperty("permalink") private String postLink;
    private int score;
    private String thumbnail;
    private boolean nsfw;
    private long created;
    private int numComments;
    private String author;
    private String url;

    private long numericId = 0;

    public Post() {
        // empty
    }

    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
        this.numericId = 0;
    }
    public String getFullId() {
        return this.fullId;
    }
    public void setFullId(String fullId) {
        this.fullId = fullId;
    }
    public long getNumericId() {
        if (this.numericId == 0 && this.id != null) {
            char[] chars = this.id.toCharArray();
            for (int i=0; i < chars.length; i++) {
                this.numericId |= ((long)(0xff & chars[i])) << (8 * i);
            }
        }
        return this.numericId;
    }
    public String getTitle() {
        return this.title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getSubreddit() {
        return this.subreddit;
    }
    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }
    public String getDomain() {
        return this.domain;
    }
    public void setDomain(String domain) {
        this.domain = domain;
    }
    public int getScore() {
        return this.score;
    }
    public void setScore(int score) {
        this.score = score;
    }
//    public int getUpVotes() {
//        return this.upVotes;
//    }
//    public void setUpVotes(int upVotes) {
//        this.upVotes = upVotes;
//    }
//    public int getDownVotes() {
//        return this.downVotes;
//    }
//    public void setDownVotes(int downVotes) {
//        this.downVotes = downVotes;
//    }
    public String getPostLink() {
        return this.postLink;
    }
    public void setPostLink(String postLink) {
        this.postLink = postLink;
    }
    public String getThumbnail() {
        return this.thumbnail;
    }
    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }
    public boolean isNsfw() {
        return this.nsfw;
    }
    public void setNsfw(boolean nsfw) {
        this.nsfw = nsfw;
    }
    public long getCreated() {
        return this.created;
    }
    public void setCreated(long created) {
        this.created = created;
    }
    public int getNumComments() {
        return this.numComments;
    }
    public void setNumComments(int numComments) {
        this.numComments = numComments;
    }
    public String getAuthor() {
        return this.author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public String getUrl() {
        return this.url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public boolean isSelfPost() {
        return this.selfPost;
    }
    public void setSelfPost(boolean selfPost) {
        this.selfPost = selfPost;
    }

    public void setSelfText(String selfText) {
        this.selfText = selfText;
    }

    public String getSelfText() {
        return this.selfText;
    }

    @Override
    public String toString() {
        return this.title + "(" + this.url + ")";
    }
}
