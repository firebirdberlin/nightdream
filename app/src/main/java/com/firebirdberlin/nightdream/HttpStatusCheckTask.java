/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.firebirdberlin.nightdream;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class HttpStatusCheckTask extends AsyncTask<String, Void, HttpStatusCheckTask.HttpStatusCheckResult> {
    private static String TAG = "NightDream.HttpStatusCheckTask";
    private static int MAX_NUM_REDIRECTS = 5;
    private static int READ_TIMEOUT = 10000;
    private static int CONNECT_TIMEOUT = 10000;

    public interface AsyncResponse {
        void onStatusCheckFinished(HttpStatusCheckTask.HttpStatusCheckResult checkResult);
    }

    public static final class HttpStatusCheckResult {
        public final String url;
        public final int responseCode;
        public final Map<String, List<String>> responseHeaders;
        public final int numRedirects;

        public boolean isSuccess() {
            return responseCode == HttpURLConnection.HTTP_OK;
        }

        public HttpStatusCheckResult(String url, int responseCode, Map<String, List<String>> responseHeaders, int numRedirects) {
            this.url = url;
            this.responseCode = responseCode;
            this.responseHeaders = responseHeaders;
            this.numRedirects = numRedirects;
        }
    }

    private AsyncResponse delegate = null;
    private int numRedirects = 0;
    private String latestURL = "";
    private int responseCode = 0;
    private Map<String, List<String>> responseHeaders;

    public HttpStatusCheckTask(AsyncResponse listener) {
        this.delegate = listener;
    }

    @Override
    protected HttpStatusCheckTask.HttpStatusCheckResult doInBackground(String... urls) {
        String url = urls[0];
        numRedirects = 0;
        responseCode = 0;
        try {
            getFinalURL(new URL(url));
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            e.printStackTrace();
        }
        //return responseCode == HttpURLConnection.HTTP_OK;
        return new HttpStatusCheckTask.HttpStatusCheckResult(latestURL, responseCode, responseHeaders, numRedirects);
    }

    @Override
    protected void onPostExecute(HttpStatusCheckTask.HttpStatusCheckResult result) {
        delegate.onStatusCheckFinished(result);
    }

    private void getFinalURL(URL url) throws IOException {
        Log.i(TAG, String.format("Checking URL %s", url.toString()));
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setInstanceFollowRedirects(false);
        con.setConnectTimeout(CONNECT_TIMEOUT);
        con.setReadTimeout(READ_TIMEOUT);
        responseCode = con.getResponseCode();
        responseHeaders = con.getHeaderFields();
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
