package com.blazeroni.json;

import static org.codehaus.jackson.JsonToken.END_ARRAY;
import static org.codehaus.jackson.JsonToken.END_OBJECT;
import static org.codehaus.jackson.JsonToken.FIELD_NAME;
import static org.codehaus.jackson.JsonToken.START_ARRAY;
import static org.codehaus.jackson.JsonToken.START_OBJECT;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonLocation;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

public class Json {
    /**
     * Finds the given JSON path, if it exists.  The {@link JsonParser} will be advanced as it attempts to find the path.
     *
     * The method will only look in nested JSON and will stop once that JSON has been exhausted.  If the parser already has
     * a current named token, that token is not considered when checking searching for the path.
     *
     * If found, the JsonParser will be advanced past the field name to the value/start object/start array token.
     *
     * @param jp
     * @param jsonPath
     * @return true if the JSON path was found in this context, false otherwise
     * @throws JsonParseException
     * @throws IOException
     */
    public static boolean findChild(JsonParser jp, String jsonPath) throws JsonParseException, IOException {
        if (StringUtils.isEmpty(jsonPath)) {
            throw new IllegalArgumentException("Must include a JSON path to find");
        }

        String[] split = StringUtils.split(jsonPath, '.');
        int index = 0;

        JsonToken current = jp.getCurrentToken();
        JsonLocation startLocation = jp.getCurrentLocation();

        if (current == null) {
            current = jp.nextToken();
            if (current == null) {
                return false;
            }
        }

        while (current == FIELD_NAME || current == END_OBJECT || current == END_ARRAY) {
            jp.nextValue();
            current = jp.getCurrentToken();
        }

        boolean found = false;
        final JsonToken end;
        if (current == START_OBJECT) {
            end = END_OBJECT;
        } else if (current == START_ARRAY) {
            end = END_ARRAY;
            jp.nextValue();
        } else {
            return found;
        }

        jp.nextValue();

        while (current != null && current != end) {
            if (split[index].equals(jp.getCurrentName()) && !jp.getCurrentLocation().equals(startLocation)) {
                index++;
                if (index == split.length)  {
                    found = true;
                    break;
                }
                current = jp.nextValue();
                if (current == START_OBJECT) {
                    current = jp.nextValue();
                }
            } else {
                jp.skipChildren();
                jp.nextValue();
                current = jp.getCurrentToken();
            }
        }

        return found;
    }

    public static boolean findSibling(JsonParser jp, String jsonPath) throws JsonParseException, IOException {
        if (StringUtils.isEmpty(jsonPath)) {
            throw new IllegalArgumentException("Must include a JSON path to find");
        }

        String[] split = StringUtils.split(jsonPath, ".", 2);

        JsonToken current = jp.getCurrentToken();

        if (current == null) {
            current = jp.nextToken();
            if (current == null) {
                return false;
            }
        }

        boolean found = false;

        while (current != null && current != END_OBJECT) {
            if (split[0].equals(jp.getCurrentName())) {
                if (split.length > 1) {
                    found = findChild(jp, split[1]);
                } else {
                    found = true;
                    jp.nextValue();
                }
                break;
            } else {
                jp.skipChildren();
                jp.nextToken();
                current = jp.getCurrentToken();
            }
        }

        return found;
    }

    /**
     * Advances the parser to the next object at the same nesting level.
     *
     * Useful for JSON arrays
     *
     * @param jp
     */
    public static void findEndObjectToken(JsonParser jp) throws JsonParseException, IOException {
        if (jp.getCurrentToken() == null) {
            jp.nextToken();
        }
        if (jp.getCurrentToken() == START_ARRAY) {
            jp.nextToken();
        }

        if (jp.getCurrentToken() == FIELD_NAME) {
            jp.nextToken();
        }

        JsonToken current = jp.getCurrentToken();
        while (current != END_OBJECT) {
            if (current == START_OBJECT || current == START_ARRAY) {
                jp.skipChildren();
                current = jp.getCurrentToken();
            } else {
                current = jp.nextValue();
            }
        }
    }
}
