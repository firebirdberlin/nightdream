package com.firebirdberlin.radiostreamapi;

import android.content.Context;

import android.util.Log;


import java.net.MalformedURLException;
import java.net.URL;

public class RadioStreamMetadataRetriever {

    private static String TAG = "RadioStreamMetadataRetriever";

    // fetch new meta infos from stream not more often than every 10 seconds
    private static final int UPDATE_INTERVAL_MILLIS = 10 * 1000;

    private long lastUpdateMillis = -1;

    private boolean updateInProgress = false;

    private boolean streamMetaDataNotSupported = false;

    private RadioStreamMetadata cachedMetadata;

    // this cache is a singleton

    private static final RadioStreamMetadataRetriever INSTANCE = new RadioStreamMetadataRetriever();

    public static RadioStreamMetadataRetriever getInstance() { return INSTANCE; }

    private RadioStreamMetadataRetriever() {}

    public void retrieveMetadata(final String streamUrl, final RadioStreamMetadataListener listener, Context context) {
        long time = System.currentTimeMillis();
        boolean needsUpdate = time - lastUpdateMillis > UPDATE_INTERVAL_MILLIS;

        Log.i(TAG, "retrieveMetadata streamMetaDataNotSupported=" + streamMetaDataNotSupported);

        if (streamMetaDataNotSupported) {
            // inform listener even if no meta data are available
            if (listener != null) {
                listener.onMetadataAvailable(null);
            }
            return;
        }

        if (!needsUpdate || updateInProgress) {
            Log.i(TAG, "cache hit");
            // cached result
            if (listener != null) {
                listener.onMetadataAvailable(cachedMetadata);
            }
            return;
        }

        Log.i(TAG, "cache miss");

        updateInProgress = true;

        // delegate to outer callback, so the progress spinner can be displayed
        if (listener != null) {
            listener.onMetadataRequestStarted();
        }

        URL url = null;
        try {
            url = new URL(streamUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        if (url != null) {

            // intermediate callback to update the cache
            StreamMetadataTask.AsyncResponse cacheCallback = new StreamMetadataTask.AsyncResponse() {

                @Override
                public void onMetadataAvailable(RadioStreamMetadata metadata) {

                    updateInProgress = false;
                    lastUpdateMillis = System.currentTimeMillis();
                    cachedMetadata = metadata;
                    streamMetaDataNotSupported = metadata.streamMetaDataNotSupported;

                    Log.i(TAG, "meta data for url:" + streamUrl);

                    // delegate to outer callback (even if no metadata available)
                    if (listener != null) {
                        listener.onMetadataAvailable(metadata);
                    }
                }

            };

            new StreamMetadataTask(cacheCallback, context).execute(url);
        }
    }

    public RadioStreamMetadata getCachedMetadata() {
        return cachedMetadata;
    }

    public void clearCache() {
        updateInProgress = false;
        lastUpdateMillis = -1;
        cachedMetadata = null;
        streamMetaDataNotSupported = false;
    }

    public interface RadioStreamMetadataListener {
        public void onMetadataRequestStarted();
        public void onMetadataAvailable(RadioStreamMetadata metadata);
    }
}
