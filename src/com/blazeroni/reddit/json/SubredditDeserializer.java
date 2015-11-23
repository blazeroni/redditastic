package com.blazeroni.reddit.json;

import java.io.IOException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import com.blazeroni.reddit.model.Post;

public class SubredditDeserializer extends JsonDeserializer<Post> {
	@Override
	public Post deserialize(JsonParser jp, DeserializationContext ctxt)
		throws IOException, JsonProcessingException {
		final Post object = new Post();

		jp.nextToken();
		JsonToken current = jp.getCurrentToken();
		while (current != null && current != JsonToken.END_OBJECT) {
			final String name = jp.getCurrentName();
			if (name == null) {
				break;
			}
			jp.nextToken();

			if ("id".equals(name)) {
                object.setId(jp.getText());
            } else if ("name".equals(name)) {
			    object.setFullId(jp.getText());
			} else if ("score".equals(name)) {
				object.setScore(jp.getCurrentToken() == JsonToken.VALUE_STRING ? Integer.valueOf(jp.getText()) : jp.getIntValue());
			} else if ("permalink".equals(name)) {
				object.setPostLink(jp.getText());
			} else if ("is_self".equals(name)) {
			    object.setSelfPost(jp.getCurrentToken() == JsonToken.VALUE_STRING ? Boolean.valueOf(jp.getText()) : jp.getBooleanValue());
			} else if ("selftext".equals(name)) {
			    object.setSelfText(jp.getText());
//			} else if ("ups".equals(name)) {
//				object.setUpVotes(jp.getCurrentToken() == JsonToken.VALUE_STRING ? Integer.valueOf(jp.getText()) : jp.getIntValue());
			} else if ("author".equals(name)) {
				object.setAuthor(jp.getText());
			} else if ("created".equals(name)) {
				object.setCreated(jp.getCurrentToken() == JsonToken.VALUE_STRING ? Long.valueOf(jp.getText()) : jp.getLongValue());
			} else if ("domain".equals(name)) {
				object.setDomain(jp.getText());
			} else if ("nsfw".equals(name)) {
				object.setNsfw(jp.getCurrentToken() == JsonToken.VALUE_STRING ? Boolean.valueOf(jp.getText()) : jp.getBooleanValue());
			} else if ("num_comments".equals(name)) {
				object.setNumComments(jp.getCurrentToken() == JsonToken.VALUE_STRING ? Integer.valueOf(jp.getText()) : jp.getIntValue());
			} else if ("subreddit".equals(name)) {
				object.setSubreddit(jp.getText());
			} else if ("thumbnail".equals(name)) {
				object.setThumbnail(jp.getText());
			} else if ("title".equals(name)) {
				object.setTitle(StringEscapeUtils.unescapeHtml4(jp.getText()));
			} else if ("url".equals(name)) {
				object.setUrl(jp.getText());
			} else if (jp.getCurrentToken() == JsonToken.START_OBJECT || jp.getCurrentToken() == JsonToken.START_ARRAY) {
				jp.skipChildren();
			}
			jp.nextToken();
			current = jp.getCurrentToken();
		}
		return object;
	}
}
