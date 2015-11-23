/*
Android Asynchronous Http Client
Copyright (c) 2011 James Smith <james@loopj.com>
http://loopj.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.blazeroni.reddit.http;

import static android.content.Context.MODE_PRIVATE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.blazeroni.reddit.util.Log;

/**
* A persistent cookie store which implements the Apache HttpClient
* {@link CookieStore} interface. Cookies are stored and will persist on the
* user's device between application sessions since they are serialized and
* stored in {@link SharedPreferences}.
* <p>
* Instances of this class are designed to be used with
* {@link AsyncHttpClient#setCookieStore}, but can also be used with a
* regular old apache HttpClient/HttpContext if you prefer.
*/
public class PersistentCookieStore implements CookieStore {
    private static final String COOKIE_PREFS = "CookiePrefsFile";
    private static final String COOKIE_NAME_STORE = "names";
    private static final String COOKIE_NAME_PREFIX = "cookie_";

    private final ConcurrentHashMap<String, Cookie> cookies;
    private final SharedPreferences cookiePrefs;

    /**
* Construct a persistent cookie store.
*/
    public PersistentCookieStore(Context context) {
        this.cookiePrefs = context.getSharedPreferences(COOKIE_PREFS, MODE_PRIVATE);
        this.cookies = new ConcurrentHashMap<String, Cookie>();

        // Load any previously stored cookies into the store
        String storedCookieNames = this.cookiePrefs.getString(COOKIE_NAME_STORE, null);

        if (storedCookieNames != null) {
            String[] cookieNames = TextUtils.split(storedCookieNames, ",");
            for (String name : cookieNames) {
                String encodedCookie = this.cookiePrefs.getString(COOKIE_NAME_PREFIX + name, null);
                if (encodedCookie != null) {
                    Cookie decodedCookie = decodeCookie(encodedCookie);
                    if (decodedCookie != null) {
                        this.cookies.put(name, decodedCookie);
                    }
                }
            }

            // Clear out expired cookies
            clearExpired(new Date());
        }
    }

    @Override
    public void addCookie(Cookie cookie) {
        String name = cookie.getName();

        Date expiryDate = cookie.getExpiryDate();
        if (Log.DEBUG) {
            Log.debug("cookie value: " + cookie.getValue());
            Log.debug("Expiry date: " + (expiryDate == null ? "no expiry date" : expiryDate.toLocaleString()));
            Log.debug("persistent: " + cookie.isPersistent());
            Log.debug("Cookie class: " + cookie.getClass().getCanonicalName());
        }
        
        // Save cookie into local store
        this.cookies.put(name, cookie);

        // Save cookie into persistent store
        SharedPreferences.Editor prefsWriter = this.cookiePrefs.edit();
        prefsWriter.putString(COOKIE_NAME_STORE, TextUtils.join(",", this.cookies.keySet()));
        prefsWriter.putString(COOKIE_NAME_PREFIX + name, encodeCookie(new SerializableCookie(cookie)));
        prefsWriter.commit();
        
    }

    @Override
    public void clear() {
        // Clear cookies from local store
        this.cookies.clear();

        // Clear cookies from persistent store
        SharedPreferences.Editor prefsWriter = this.cookiePrefs.edit();
        prefsWriter.clear();
        prefsWriter.commit();
    }

    @Override
    public boolean clearExpired(Date date) {
        boolean clearedAny = false;
        SharedPreferences.Editor prefsWriter = this.cookiePrefs.edit();

        for (ConcurrentHashMap.Entry<String, Cookie> entry : this.cookies.entrySet()) {
            String name = entry.getKey();
            Cookie cookie = entry.getValue();
            if (cookie.isExpired(date)) {
                // Clear cookies from local store
                this.cookies.remove(name);

                // Clear cookies from persistent store
                prefsWriter.remove(COOKIE_NAME_PREFIX + name);

                // We've cleared at least one
                clearedAny = true;
            }
        }

        // Update names in persistent store
        if (clearedAny) {
            prefsWriter.putString(COOKIE_NAME_STORE, TextUtils.join(",", this.cookies.keySet()));
            prefsWriter.commit();
        }

        return clearedAny;
    }

    @Override
    public List<Cookie> getCookies() {
        return new ArrayList<Cookie>(this.cookies.values());
    }


    //
    // Cookie serialization/deserialization
    //

    protected String encodeCookie(SerializableCookie cookie) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(os);
            outputStream.writeObject(cookie);
        } catch (Exception e) {
        	Log.error("Failed to encode cookie", e);
            return null;
        }

        return byteArrayToHexString(os.toByteArray());
    }

    protected Cookie decodeCookie(String cookieStr) {
        byte[] bytes = hexStringToByteArray(cookieStr);
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        Cookie cookie = null;
        try {
        	ObjectInputStream ois = new ObjectInputStream(is);
        	cookie = ((SerializableCookie)ois.readObject()).getCookie();
        } catch (Exception e) {
        	Log.error("Failed to decode cookie!", e);
        }

        return cookie;
    }

    // Using some super basic byte array <-> hex conversions so we don't have
    // to rely on any large Base64 libraries. Can be overridden if you like!
    protected String byteArrayToHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte element : b) {
            int v = element & 0xff;
            if(v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase(Locale.US);
    }

    protected byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}