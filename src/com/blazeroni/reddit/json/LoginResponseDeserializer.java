package com.blazeroni.reddit.json;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import com.blazeroni.json.Json;
import com.blazeroni.reddit.model.LoginResponse;

public class LoginResponseDeserializer extends JsonDeserializer<LoginResponse> {
	@Override
	public LoginResponse deserialize(JsonParser jp, DeserializationContext ctxt)
		throws IOException, JsonProcessingException {
		final LoginResponse object = new LoginResponse();

		if (Json.findChild(jp, "json")) {
		    jp.nextToken();
            JsonToken current = jp.getCurrentToken();
            while (current != null && current != JsonToken.END_OBJECT) {
                final String name = jp.getCurrentName();
                if (name == null) {
                    break;
                }
                jp.nextToken();

                if ("errors".equals(name)) {
                    deserializeErrors(jp, object);
                } else if ("data".equals(name)) {
                    deserializeData(jp, object);
                } else if (jp.getCurrentToken() == JsonToken.START_OBJECT || jp.getCurrentToken() == JsonToken.START_ARRAY) {
                    jp.skipChildren();
                }
                jp.nextToken();
                current = jp.getCurrentToken();
            }
		}
		return object;
	}

	private void deserializeErrors(JsonParser jp, LoginResponse object) throws IOException, JsonProcessingException {
	    while (jp.getCurrentToken() != JsonToken.START_ARRAY) {
	        jp.nextToken();
	    }

        JsonToken current = jp.getCurrentToken();
        while (current != null && current != JsonToken.END_ARRAY) {
            while (jp.getCurrentToken() == JsonToken.START_ARRAY) {
                jp.nextToken();
            }

            if (jp.getCurrentToken() == JsonToken.END_ARRAY) {
                // no errors
                jp.nextToken();
                break;
            }

            String error = jp.getText();

            object.addError(error);

            while (jp.getCurrentToken() != JsonToken.END_ARRAY) {
                jp.nextToken();
            }
            jp.nextToken();
            current = jp.getCurrentToken();
        }
	}

	private void deserializeData(JsonParser jp, LoginResponse object) throws IOException, JsonProcessingException {
        JsonToken current = jp.getCurrentToken();
        while (current != null && current != JsonToken.END_OBJECT) {
            final String name = jp.getCurrentName();
            if (name == null) {
                break;
            }
            jp.nextToken();

            if ("modhash".equals(name)) {
                object.setModhash(jp.getText());
            } else if ("cookie".equals(name)) {
                object.setCookie(jp.getText());
            } else if (jp.getCurrentToken() == JsonToken.START_OBJECT || jp.getCurrentToken() == JsonToken.START_ARRAY) {
                jp.skipChildren();
            }
            jp.nextToken();
            current = jp.getCurrentToken();
        }
    }
}
