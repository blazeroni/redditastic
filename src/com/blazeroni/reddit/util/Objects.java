package com.blazeroni.reddit.util;

public class Objects {
    @SuppressWarnings("unchecked")
    public static <T> T ensureClass(Object obj, Class<T> clazz) {
        if (!clazz.isInstance(obj)) {
            throw new IllegalArgumentException("object is not an instance of class [" + clazz.getCanonicalName() + "]");
        }
        return (T) obj;
    }
}
