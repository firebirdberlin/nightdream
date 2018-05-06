package com.firebirdberlin.radiostreamapi;

import android.content.Context;

import android.util.Log;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class IcecastMetadataCache {

    private static String TAG = "IcecastMetadataCache";

    private static final int UPDATE_INTERVAL_MILLIS = 60 * 1000;
    //private static final int UPDATE_INTERVAL_MILLIS = 5 * 1000;

    // forced updates (long press) at most every 3 seconds
    private static final int FORCED_UPDATE_INTERVAL_MILLIS = 3 * 1000;

    private long lastUpdateMillis = -1;

    private boolean updateInProgress = false;

    private boolean radioStationMetaDataAvaiable = false;

    private IcecastMetadata cachedMetadata;

    public void retrieveMetadata(final String streamUrl, final StreamMetadataTask.AsyncResponse listener, Context context, boolean forceUpdate) {
        long time = System.currentTimeMillis();
        boolean needsUpdate = time - lastUpdateMillis > UPDATE_INTERVAL_MILLIS;
        boolean needsForcedUpdate = time - lastUpdateMillis > FORCED_UPDATE_INTERVAL_MILLIS;

        if (!radioStationMetaDataAvaiable) {
            // inform listener even if no meta data are available
            if (listener != null) {
                listener.onMetadataAvailable(null);
            }
            return;
        }

        if ((!needsUpdate && !forceUpdate) || updateInProgress) {
            Log.i(TAG, "cache hit");
            // cached result
            if (listener != null) {
                listener.onMetadataAvailable(cachedMetadata);
            }
            return;
        }

        if ((!needsForcedUpdate && forceUpdate) || updateInProgress) {
            Log.i(TAG, "cache hit");
            // cached result
            if (listener != null) {
                // for forced updates simulate a real request so on-pregress occures
                listener.onMetadataRequestStarted();
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
                public void onMetadataRequestStarted() {
                    // nothing to do here
                }

                @Override
                public void onMetadataAvailable(IcecastMetadata metadata) {

                    updateInProgress = false;
                    lastUpdateMillis = System.currentTimeMillis();
                    cachedMetadata = metadata;

                    Log.i(TAG, "meta data for url:" + streamUrl);
                    radioStationMetaDataAvaiable = false;

                    String streamTitle = (metadata != null ? metadata.streamTitle : null);

                    if (streamTitle != null && !streamTitle.isEmpty()) {
                        radioStationMetaDataAvaiable = true;
                    }

                    // delegate to outer callback (even if no metadata available)
                    if (listener != null) {
                        listener.onMetadataAvailable(metadata);
                    }
                }

            };

            new StreamMetadataTask(cacheCallback, context).execute(url);
        }
    }

    public IcecastMetadata getCachedMetadata() {
        return cachedMetadata;
    }

    public void invalidate() {
        updateInProgress = false;
        lastUpdateMillis = -1;
        cachedMetadata = null;
        radioStationMetaDataAvaiable = true;
    }

}
