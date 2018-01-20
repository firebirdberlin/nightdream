package com.firebirdberlin.nightdream;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpStatusCheckTask extends AsyncTask<String, Void, Boolean> {
    private static String TAG = "NightDream.HttpStatusCheckTask";
    private static int MAX_NUM_REDIRECTS = 5;
    private static int READ_TIMEOUT = 10000;
    private static int CONNECT_TIMEOUT = 10000;

    public interface AsyncResponse {
        public void onStatusCheckFinished(Boolean success, String url, int numRedirects);
    }

    private AsyncResponse delegate = null;
    private int numRedirects = 0;
    private String latestURL = "";
    private int responseCode = 0;

    public HttpStatusCheckTask(AsyncResponse listener) {
        this.delegate = listener;
    }

    @Override
    protected Boolean doInBackground(String... urls) {
        String url = urls[0];
        numRedirects = 0;
        responseCode = 0;
        try {
            getFinalURL(new URL(url));
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            e.printStackTrace();
        }
        return responseCode == HttpURLConnection.HTTP_OK;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        delegate.onStatusCheckFinished(success, latestURL, numRedirects);
    }

    private void getFinalURL(URL url) throws IOException {
        Log.i(TAG, String.format("Checking URL %s", url.toString()));
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setInstanceFollowRedirects(false);
        con.setConnectTimeout(CONNECT_TIMEOUT);
        con.setReadTimeout(READ_TIMEOUT);
        responseCode = con.getResponseCode();
        latestURL = url.toString();
        if (numRedirects < MAX_NUM_REDIRECTS &&
                (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP)) {
            numRedirects++;
            String redirectUrl = con.getHeaderField("Location");
            getFinalURL(new URL(redirectUrl));
        }
    }
}
