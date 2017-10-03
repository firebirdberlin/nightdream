package com.firebirdberlin.radiostreamapi;

import android.os.AsyncTask;

public class StreamURLAvailabilityCheckTask extends AsyncTask<String, Void, Boolean> {

    public interface AsyncResponse {
        public void onRequestFinished(Boolean result);
    }

    private AsyncResponse delegate = null;

    public StreamURLAvailabilityCheckTask(AsyncResponse listener) {
        this.delegate = listener;
    }

    @Override
    protected Boolean doInBackground(String... query) {
        String url = query[0];
        return PlaylistParser.checkStreamURLAvailability(url);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        delegate.onRequestFinished(result);
    }
}
