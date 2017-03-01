package com.firebirdberlin.nightdream;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpStatusCheckTask extends AsyncTask<String, Void, Boolean> {
    private static String TAG = "NightDream.HttpStatusCheckTask";

    public interface AsyncResponse {
        public void onStatusCheckFinished(Boolean success);
    }

    private AsyncResponse delegate = null;

    public HttpStatusCheckTask(AsyncResponse listener) {
        this.delegate = listener;
    }

    @Override
    protected Boolean doInBackground(String... urls) {
        String url = urls[0];
        return checkStatus(url);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        delegate.onStatusCheckFinished(success);
    }

    private boolean checkStatus(String urlString) {
        try {
            Log.i(TAG, String.format("Checking %s", urlString));
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            //response = urlConnection.getResponseMessage();
            int responseCode = urlConnection.getResponseCode();
            Log.i(TAG, String.format("response %d", responseCode));
            return (responseCode == 200);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            e.printStackTrace();
            return false;
        }
    }
}
