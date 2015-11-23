package com.blazeroni.reddit.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Handler;
import android.util.Log;

/**
 * A class similar but unrelated to android's {@link android.os.AsyncTask}.
 *
 * Unlike AsyncTask, this class properly propagates exceptions.
 *
 * If you're familiar with AsyncTask and are looking for {@link android.os.AsyncTask#doInBackground(Object[])},
 * we've named it {@link #call()} here to conform with java 1.5's {@link java.util.concurrent.Callable} interface.
 *
 * Current limitations: does not yet handle progress, although it shouldn't be
 * hard to add.
 *
 * Borrowed from Roboguice.
 *
 * @param <T>
 */
public abstract class SafeAsyncTask<T> implements Callable<T> {

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "SafeAsyncTask #" + this.mCount.getAndIncrement());
        }
    };
    private static final ExecutorService POOL = Executors.newCachedThreadPool(THREAD_FACTORY);

    private Handler handler;
    private FutureTask<T> future = new FutureTask<T>(newTask());

    public SafeAsyncTask() {
        this.handler = new Handler();
    }

    public SafeAsyncTask(Handler handler) {
        this.handler = handler;
    }

    public void execute() {
        POOL.execute(this.future);
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.future != null && this.future.cancel(mayInterruptIfRunning);
    }

    /**
     * @throws Exception, captured on passed to onException() if present.
     */
    protected void onPreExecute() throws Exception {}

    /**
     * @param t the result of {@link #call()}
     * @throws Exception, captured on passed to onException() if present.
     */
    protected void onSuccess(T t) throws Exception {}

    /**
     * Called when the thread has been interrupted, likely because
     * the task was canceled.
     *
     * By default, calls {@link #onException(Exception)}, but this method
     * may be overridden to handle interruptions differently than other
     * exceptions.
     *
     * @param e the exception thrown from {@link #onPreExecute()}, {@link #call()}, or {@link #onSuccess(Object)}
     */
    protected void onInterrupted(InterruptedException e) {
        onException(e);
    }

    /**
     * Logs the exception as an Error by default, but this method may
     * be overridden by subclasses.
     *
     * @param e the exception thrown from {@link #onPreExecute()}, {@link #call()}, or {@link #onSuccess(Object)}
     * @throws RuntimeException, ignored
     */
    protected void onException( Exception e ) throws RuntimeException {
        Log.e("Redditastic", "Exception caught during background processing", e);
    }

    /**
     * @throws RuntimeException, ignored
     */
    protected void onFinally() throws RuntimeException {}


    protected Task newTask() {
        return new Task();
    }

    protected class Task implements Callable<T> {
        public T call() throws Exception {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            try {
                doPreExecute();
                doSuccess(doCall());
                return null;

            } catch( final Exception e ) {
                try {
                    doException(e);
                } catch( Exception f ) {
                    // ignored, throw original instead
                }
                throw e;

            } finally {
                doFinally();
            }
        }

        protected void doPreExecute() throws Exception {
            postToUiThreadAndWait( new Callable<Object>() {
                public Object call() throws Exception {
                    onPreExecute();
                    return null;
                }
            });
        }

        protected T doCall() throws Exception {
            return SafeAsyncTask.this.call();
        }

        protected void doSuccess( final T r ) throws Exception {
            postToUiThreadAndWait( new Callable<Object>() {
                public Object call() throws Exception {
                    onSuccess(r);
                    return null;
                }
            });
        }

        protected void doException( final Exception e ) throws Exception {
            postToUiThreadAndWait( new Callable<Object>() {
                public Object call() throws Exception {
                    if( e instanceof InterruptedException ) {
                        onInterrupted((InterruptedException)e);
                    }
                    else {
                        onException(e);
                    }
                    return null;
                }
            });
        }

        protected void doFinally() throws Exception {
            postToUiThreadAndWait( new Callable<Object>() {
                public Object call() throws Exception {
                    onFinally();
                    return null;
                }
            });
        }

        /**
         * Posts the specified runnable to the UI thread using a handler,
         * and waits for operation to finish.  If there's an exception,
         * it captures it and rethrows it.
         * @param c the callable to post
         * @throws Exception on error
         */
        protected void postToUiThreadAndWait( final Callable<?> c ) throws Exception {
            final CountDownLatch latch = new CountDownLatch(1);
            final Exception[] exceptions = new Exception[1];

            // Execute onSuccess in the UI thread, but wait
            // for it to complete.
            // If it throws an exception, capture that exception
            // and rethrow it later.
            SafeAsyncTask.this.handler.post( new Runnable() {
               public void run() {
                   try {
                       c.call();
                   } catch( Exception e ) {
                       exceptions[0] = e;
                   } finally {
                       latch.countDown();
                   }
               }
            });

            // Wait for onSuccess to finish
            latch.await();

            if( exceptions[0] != null ) {
                throw exceptions[0];
            }
        }
    }
}
