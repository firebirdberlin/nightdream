package com.firebirdberlin.radiostreamapi;

import android.content.Context;
import android.os.AsyncTask;

import java.net.URL;
import java.util.Map;

public class StreamMetadataTask extends AsyncTask<URL, Void, Map<String, String>> {

    public interface AsyncResponse {
        public void onMetadataRequestStarted();
        public void onMetadataAvailable(Map<String, String> metadata);
    }

    private StreamMetadataTask.AsyncResponse delegate = null;
    private Context context;

    public StreamMetadataTask(StreamMetadataTask.AsyncResponse listener, Context context) {
        this.delegate = listener;
        this.context = context;
    }

    @Override
    protected Map<String, String> doInBackground(URL... params) {

        Map<String, String> metadata = IcecastMetadataRetriever.retrieveMetadata(params[0]);
        return metadata;
    }

    @Override
    protected void onPostExecute(Map<String, String> metadata) {
        delegate.onMetadataAvailable(metadata);
    }

}
