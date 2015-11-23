package com.blazeroni.reddit.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.util.Base64;
import android.util.DisplayMetrics;

import com.blazeroni.reddit.RedditApp;
import com.blazeroni.reddit.model.PostSort;
import com.blazeroni.reddit.util.Log;

public class Http {
    private static final String RETRY_HANDLER = "http.method.retry-handler";

    public static final int SOCKET_TIMEOUT = 5 * 1000;
    public static final int MAX_CONNECTIONS_PER_ROUTE = 8;
    public static final int MAX_CONNECTIONS = 30;
    public static final int MAX_RETRIES = 3;

    private static String USER_AGENT = "redditastic/1.1  Android/" + Build.VERSION.SDK_INT;

    private static String PARAM_MODHASH = "uh";

    private static AndroidHttpClient client = AndroidHttpClient.newInstance(USER_AGENT);
    private static HttpContext context = new BasicHttpContext();

    static {
        HttpParams params = client.getParams();
        params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(MAX_CONNECTIONS_PER_ROUTE));
        params.setIntParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, MAX_CONNECTIONS);

        ConnManagerParams.setTimeout(params, SOCKET_TIMEOUT);
        ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRouteBean(MAX_CONNECTIONS_PER_ROUTE));
        ConnManagerParams.setMaxTotalConnections(params, MAX_CONNECTIONS);

        HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
        HttpConnectionParams.setTcpNoDelay(params, true);

        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

        params.setParameter(RETRY_HANDLER, new RetryHandler(MAX_RETRIES));

        context.setAttribute(ClientContext.COOKIE_STORE, new PersistentCookieStore(RedditApp.getContext()));
    }

    public static void deleteCookies() {
        CookieStore cookieStore = (CookieStore) context.getAttribute(ClientContext.COOKIE_STORE);
        if (cookieStore != null) {
            cookieStore.clear();
        }
    }

    public static InputStream login(String username, String password) throws ClientProtocolException, IOException {
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>(3);
        params.add(pair("user", username));
        params.add(pair("passwd", password));
        params.add(pair("rem", "true"));
        params.add(pair("api_type", "json"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
        return postStream("https://ssl.reddit.com/api/login/" + username, entity);
    }

    public static InputStream fetchSubscribedSubreddits() throws ClientProtocolException, IOException {
        return getStream("http://www.reddit.com/reddits/mine.json");
    }

    public static InputStream fetchSubreddit(String subreddit, PostSort sort) throws ClientProtocolException, IOException {
        if (Log.DEBUG) {
            Log.debug("http://www.reddit.com" + subreddit + sort.getPath() + ".json?" + sort.getQuery());
        }
        return getStream("http://www.reddit.com" + subreddit + sort.getPath() + ".json?" + sort.getQuery());
    }

    public static InputStream fetchImage(String url) throws ClientProtocolException, IOException {
        if (url.startsWith("/")) {
            url = "http://www.reddit.com" + url;
        }

        return getStream(url);
    }

    public static BitmapDrawable fetchImage(String url, int density) throws ClientProtocolException, IOException {
        if (url.startsWith("/")) {
            url = "http://www.reddit.com" + url;
        }

        InputStream stream = null;
        try {
            stream = getStream(url);

            Options options = new Options();
            options.inDensity = DisplayMetrics.DENSITY_MEDIUM;
            options.inTargetDensity = density;

            return new BitmapDrawable(BitmapFactory.decodeStream(stream, null, options));
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    private static InputStream getStream(String url) throws ClientProtocolException, IOException {
        HttpGet request = new HttpGet(url);
        AndroidHttpClient.modifyRequestToAcceptGzipResponse(request);

        HttpResponse response = client.execute(request, context);

        HttpEntity entity = response.getEntity();
        return AndroidHttpClient.getUngzippedContent(entity);
    }

    public static HttpContent getContent(String url) throws ClientProtocolException, IOException {
        HttpGet request = new HttpGet(url);
        AndroidHttpClient.modifyRequestToAcceptGzipResponse(request);

        HttpResponse response = client.execute(request, context);

        return new HttpContent(url, response.getEntity());
    }

    private static InputStream postStream(String url, HttpEntity data) throws ClientProtocolException, IOException {
        HttpPost request = new HttpPost(url);
        request.setEntity(data);

        AndroidHttpClient.modifyRequestToAcceptGzipResponse(request);

        HttpResponse response = client.execute(request, context);

        HttpEntity entity = response.getEntity();
        return AndroidHttpClient.getUngzippedContent(entity);
    }

    private static NameValuePair pair(String name, String value) {
        return new BasicNameValuePair(name, value);
    }

    public static class HttpContent {
        String url;
        String content;
        String mimeType;
        String encoding;

        public HttpContent(String url, HttpEntity entity) throws IOException {
            this.url = url;
            this.content = "<img src=\"data:image/jpeg;base64, " + Base64.encodeToString(IOUtils.toByteArray(AndroidHttpClient.getUngzippedContent(entity)), Base64.DEFAULT) + "\" />";
            this.mimeType = entity.getContentType().getValue();
            Header header = entity.getContentEncoding();
            if (header != null) {
                this.encoding = header.getValue();
            }
        }

        public String getUrl() {
            return this.url;
        }

        public String getContent() {
            return this.content;
        }

        public String getMimeType() {
            return this.mimeType;
        }

        public String getEncoding() {
            return this.encoding;
        }
    }
}
