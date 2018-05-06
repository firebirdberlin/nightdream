package com.firebirdberlin.radiostreamapi;

import android.content.Context;
import android.os.AsyncTask;

import java.net.URL;
import java.util.Map;

public class StreamMetadataTask extends AsyncTask<URL, Void, IcecastMetadata> {

    public interface AsyncResponse {
        public void onMetadataRequestStarted();
        public void onMetadataAvailable(IcecastMetadata metadata);
    }

    private StreamMetadataTask.AsyncResponse delegate = null;
    private Context context;

    public StreamMetadataTask(StreamMetadataTask.AsyncResponse listener, Context context) {
        this.delegate = listener;
        this.context = context;
    }

    @Override
    protected IcecastMetadata doInBackground(URL... params) {

        IcecastMetadata metadata = IcecastMetadataRetriever.retrieveMetadata(params[0]);
        return metadata;
    }

    @Override
    protected void onPostExecute(IcecastMetadata metadata) {
        delegate.onMetadataAvailable(metadata);
    }

}
