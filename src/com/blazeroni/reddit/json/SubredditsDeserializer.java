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
import com.blazeroni.reddit.model.Subreddits;

public class SubredditsDeserializer extends JsonDeserializer<Subreddits> {
    @Override
    public Subreddits deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        final Subreddits subreddits = new Subreddits();

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
                    ArrayList<String> list = new ArrayList<String>();
                    while (Json.findChild(jp, "data.display_name")) {
                        String subreddit = jp.getText();
                        if (subreddit != null) {
                            list.add(subreddit);
                        }
                        Json.findEndObjectToken(jp);
                    }
                    subreddits.setSubreddits(list);
                    continue;
                } else if (jp.getCurrentToken() == JsonToken.START_OBJECT || jp.getCurrentToken() == JsonToken.START_ARRAY) {
                    jp.skipChildren();
                }

                jp.nextValue();
                current = jp.getCurrentToken();
            }
        }

        return subreddits;
    }
}
