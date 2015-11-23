package com.blazeroni.reddit.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.Log;

import com.blazeroni.reddit.http.Http;

public class ImageLoader {
    private static final String TAG = "ImageLoader";
    private static final int CORE_POOL_SIZE = 4;
    private static final int MAXIMUM_POOL_SIZE = 10;
    private static final int KEEP_ALIVE = 5;

    private static final BitmapDrawable EMPTY_IMAGE = new BitmapDrawable();

    private static final ThreadFactory threadFactory = new ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "ImageLoader #" + this.count.getAndIncrement());
        }
    };

    private Executor executor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                                    KEEP_ALIVE, TimeUnit.SECONDS,
                                    new LinkedBlockingQueue<Runnable>(), threadFactory);

    private ConcurrentHashMap<Integer,BitmapDrawable> cache = new ConcurrentHashMap<Integer,BitmapDrawable>();

    private final File cacheDir;
    private final int density;

    public interface Callback {
        public void imageLoaded(String url, Drawable image);
        public void imageError(String url);
    }

    public ImageLoader(Context context) {
        this.cacheDir = new File(context.getCacheDir().getAbsolutePath() + File.separator + "images");
        if (!this.cacheDir.exists()) {
            boolean created = this.cacheDir.mkdirs();
            if (!created) {
                Log.w(TAG, "Couldn't create image cache directory");
            }
        }
        this.density = context.getResources().getDisplayMetrics().densityDpi;
        Log.d(TAG, "Density: " + this.density);
    }

    public Drawable get(String url) {
        final int hashCode = url.hashCode();
        BitmapDrawable image = this.cache.putIfAbsent(hashCode, EMPTY_IMAGE);
        if (isValid(image)) {
            return image;
        }

        final File file = new File(ImageLoader.this.cacheDir.getAbsoluteFile() + File.separator + hashCode);
        image = loadFromFile(file);
        if (image != null) {
            this.cache.put(hashCode, image);
        } else {
            this.cache.remove(hashCode);
        }
        return image;
    }

    private boolean isValid(BitmapDrawable image) {
        if (image != null && image != EMPTY_IMAGE) {
            Bitmap bitmap = image.getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                return true;
            }
        }

        return false;
    }

    public Drawable load(final String url, final Callback callback) {
        final int hashCode = url.hashCode();
        BitmapDrawable image = this.cache.putIfAbsent(hashCode, EMPTY_IMAGE);
        if (isValid(image)) {
            callback.imageLoaded(url, image);
            return image;
        }

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                BitmapDrawable image = null;
                InputStream stream = null;
                try {
                    final File file = new File(ImageLoader.this.cacheDir.getAbsoluteFile() + File.separator + hashCode);
                    if (file.exists()) {
                        image = loadFromFile(file);
                    } else {
                        stream = Http.fetchImage(url);

                        byte[] data = IOUtils.toByteArray(stream);
                        FileUtils.writeByteArrayToFile(file, data);

                        Options options = new Options();
                        options.inDensity = DisplayMetrics.DENSITY_MEDIUM;
                        options.inTargetDensity = ImageLoader.this.density;

                        Bitmap bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(data), null, options);
                        // it's possible the bitmap could have failed to be created
                        if (bitmap != null) {
                            image = new BitmapDrawable(bitmap);
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to load image: " + url, e);
                } finally {
                    IOUtils.closeQuietly(stream);
                }
                if (image != null) {
                    ImageLoader.this.cache.put(hashCode, image);
                    callback.imageLoaded(url, image);
                } else {
                    ImageLoader.this.cache.remove(hashCode);
                    callback.imageError(url);
                }
            }
        });

        return null;
    }

    private BitmapDrawable loadFromFile(File file) {
        try {
            if (file.exists()) {
                Options options = new Options();
                options.inDensity = DisplayMetrics.DENSITY_MEDIUM;
                options.inTargetDensity = ImageLoader.this.density;

                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                if (bitmap != null) {
                    BitmapDrawable drawable = new BitmapDrawable(bitmap);
                    return drawable;
                } else {
                    // loading bitmap failed - remove file
                    FileUtils.deleteQuietly(file);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed loading image from file: " + file.getAbsolutePath(), e);
        }
        return null;
    }


    public void cancel(String url) {

    }

    public void cancelAll() {

    }
}
