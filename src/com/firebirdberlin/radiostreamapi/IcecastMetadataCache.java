package com.firebirdberlin.radiostreamapi;

import android.content.Context;

import android.util.Log;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class IcecastMetadataCache {

    private static String TAG = "IcecastMetadataCache";

    //private static final int UPDATE_INTERVAL_MILLIS = 60 * 1000;
    private static final int UPDATE_INTERVAL_MILLIS = 5 * 1000;

    private long lastUpdateMillis = -1;

    private boolean updateInProgress = false;

    private boolean radioStationMetaDataAvaiable = false;

    private Map<String, String> cachedMetadata;

    public void retrieveMetadata(final String streamUrl, final StreamMetadataTask.AsyncResponse listener, Context context) {
        long time = System.currentTimeMillis();
        boolean needsUpdate = time - lastUpdateMillis > UPDATE_INTERVAL_MILLIS;

        if (!radioStationMetaDataAvaiable) {
            return;
        }

        if (!needsUpdate || updateInProgress) {
            Log.i(TAG, "cache hit");
            // cached result
            if (listener != null) {
                listener.onMetadataRequestFinished(cachedMetadata);
            }
            return;
        }

        Log.i(TAG, "cache miss");

        updateInProgress = true;


        URL url = null;
        try {
            url = new URL(streamUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        if (url != null) {
            StreamMetadataTask.AsyncResponse cacheCallback = new StreamMetadataTask.AsyncResponse() {

                @Override
                public void onMetadataRequestFinished(Map<String, String> metadata) {

                    updateInProgress = false;
                    lastUpdateMillis = System.currentTimeMillis();
                    cachedMetadata = metadata;

                    Log.i(TAG, "meta data for url:" + streamUrl);
                    radioStationMetaDataAvaiable = false;
                    if (metadata != null && !metadata.isEmpty() && metadata.containsKey(IcecastMetadataRetriever.META_KEY_STREAM_TITLE)) {

                        String title = metadata.get(IcecastMetadataRetriever.META_KEY_STREAM_TITLE);
                        if (!title.isEmpty()) {
                            radioStationMetaDataAvaiable = true;
                        }

                    }

                    if (listener != null) {
                        listener.onMetadataRequestFinished(metadata);
                    }
                }

            };

            new StreamMetadataTask(cacheCallback, context).execute(url);
        }
    }

    public void invalidate() {
        updateInProgress = false;
        lastUpdateMillis = -1;
        cachedMetadata = null;
        radioStationMetaDataAvaiable = true;
    }

}
