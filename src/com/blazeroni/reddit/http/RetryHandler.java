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

/*
Some of the retry logic in this class is heavily borrowed from the
fantastic droid-fu project: https://github.com/donnfelker/droid-fu
*/

package com.blazeroni.reddit.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import android.os.SystemClock;

import com.blazeroni.reddit.util.Log;

class RetryHandler implements HttpRequestRetryHandler {
    private static final int RETRY_SLEEP_TIME_MILLIS = 1000;
    private static final HashMap<Class<?>, Boolean> EXCEPTION_MAP = new HashMap<Class<?>, Boolean>();

    static {
        // Retry if the server dropped connection on us
        EXCEPTION_MAP.put(NoHttpResponseException.class, Boolean.TRUE);
        // retry-this, since it may happens as part of a Wi-Fi to 3G failover
        EXCEPTION_MAP.put(UnknownHostException.class, Boolean.TRUE);
        // retry-this, since it may happens as part of a Wi-Fi to 3G failover
        EXCEPTION_MAP.put(SocketException.class, Boolean.TRUE);

        // never retry timeouts
        EXCEPTION_MAP.put(InterruptedIOException.class, Boolean.FALSE);
        // never retry SSL handshake failures
        EXCEPTION_MAP.put(SSLHandshakeException.class, Boolean.FALSE);
    }

    private final int maxRetries;

    public RetryHandler(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
        boolean retry;

        Boolean b = (Boolean) context.getAttribute(ExecutionContext.HTTP_REQ_SENT);
        boolean sent = (b != null && b.booleanValue());

        if(executionCount > this.maxRetries) {
            // Do not retry if over max retry count
            retry = false;
        } else if (EXCEPTION_MAP.containsKey(exception.getClass())) {
            // immediately cancel retry if the error is blacklisted
            // immediately retry if error is whitelisted
            return EXCEPTION_MAP.get(exception.getClass());
        } else if (!sent) {
            // for most other errors, retry only if request hasn't been fully sent yet
            retry = true;
        } else {
            // resend all idempotent requests
            HttpUriRequest currentReq = (HttpUriRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
            String requestType = currentReq.getMethod();
            if(!requestType.equals("POST")) {
                retry = true;
            } else {
                // otherwise do not retry
                retry = false;
            }
        }

        if (retry) {
            SystemClock.sleep(RETRY_SLEEP_TIME_MILLIS);
        } else {
            Log.debug("Not retrying request", exception);
        }

        return retry;
    }
}