package com.blazeroni.reddit.json;

import static org.codehaus.jackson.JsonToken.END_OBJECT;

import java.io.IOException;
import java.util.ArrayList;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import com.blazeroni.json.Json;
import com.blazeroni.reddit.model.Post;
import com.blazeroni.reddit.model.Posts;

public class PostsDeserializer extends JsonDeserializer<Posts> {
    @Override
    public Posts deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        final Posts posts = new Posts();

        if (Json.findChild(jp, "data")) {
            jp.nextToken();
            JsonToken current = jp.getCurrentToken();
            String name = jp.getCurrentName();
            while (current != null && current != END_OBJECT) {
                name = jp.getCurrentName();
                if (name == null) {
                    break;
                }

                if ("children".equals(name)) {
                    ArrayList<Post> list = new ArrayList<Post>();
                    while (Json.findChild(jp, "data")) {
                        Post post = jp.readValueAs(Post.class);
                        if (post != null) {
                            list.add(post);
                        }
                    }
                    posts.setPosts(list);
                    continue;
                } else if ("after".equals(name)) {
                    posts.setNext(jp.getCurrentToken() == JsonToken.VALUE_STRING ? jp.getText() : null);
                } else if (jp.getCurrentToken() == JsonToken.START_OBJECT || jp.getCurrentToken() == JsonToken.START_ARRAY) {
                    jp.skipChildren();
                }

                jp.nextValue();
                current = jp.getCurrentToken();
            }
        }

        return posts;
    }
}
