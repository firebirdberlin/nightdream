package com.firebirdberlin.radiostreamapi;

import android.content.Context;
import android.os.AsyncTask;

import java.net.URL;

public class StreamMetadataTask extends AsyncTask<URL, Void, RadioStreamMetadata> {

    public interface AsyncResponse {
        void onMetadataAvailable(RadioStreamMetadata metadata);
    }

    private StreamMetadataTask.AsyncResponse delegate;
    private Context context;

    public StreamMetadataTask(StreamMetadataTask.AsyncResponse listener, Context context) {
        this.delegate = listener;
        this.context = context;
    }

    @Override
    protected RadioStreamMetadata doInBackground(URL... params) {

        RadioStreamMetadata metadata = IcecastMetadataRetriever.retrieveMetadata(params[0]);
        return metadata;
    }

    @Override
    protected void onPostExecute(RadioStreamMetadata metadata) {
        delegate.onMetadataAvailable(metadata);
    }

}
