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

package com.firebirdberlin;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import com.firebirdberlin.nightdream.BuildConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

public class HttpReader {
    private static final String TAG = "HttpReader";

    private final static int READ_TIMEOUT = 10000;
    private final static int CONNECT_TIMEOUT = 10000;
    private final File cacheBaseDir;
    private final Context context;
    private final long unsuccessfulAttemptTimeout = 1000 * 60 * 10; // 10 Minutes
    private long requestTimestamp;
    private long cacheExpirationTimeMillis = 1000 * 60 * 60 * 24;

    public HttpReader(Context context, final String cacheFileName) {
        this.cacheBaseDir = new File(context.getCacheDir(), cacheFileName);
        if (!this.cacheBaseDir.exists()) {
            this.cacheBaseDir.mkdirs();
        }
        this.context = context;
    }

    private File getCacheFileForUrl(String urlString) {
        // Using hashCode as a simple way to get a unique-ish filename
        // A more robust solution might use a cryptographic hash (e.g., MD5) or URL encoding.
        String uniqueFileName = String.valueOf(urlString.hashCode());
        return new File(cacheBaseDir, uniqueFileName);
    }

    private File getLockFileForUrl(String urlString) {
        String uniqueFileName = String.valueOf(urlString.hashCode());
        return new File(cacheBaseDir, uniqueFileName + ".lock~");
    }

    private static String getResponseText(InputStream inputStream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    private static void storeCacheFile(File cacheFile, String responseText) {
        try {
            FileOutputStream stream = new FileOutputStream(cacheFile);
            stream.write(responseText.getBytes());
            stream.close();
            Log.i(TAG, cacheFile.toString() + " stored");
        } catch (IOException e) {
            if (cacheFile.exists()) {
                cacheFile.delete();
            }
        }
    }

    private static String readFromCacheFile(File cacheFile) {
        int length = (int) cacheFile.length();
        byte[] bytes = new byte[length];

        try {
            FileInputStream in = new FileInputStream(cacheFile);
            in.read(bytes);
            in.close();
        } catch (IOException e) {
            return null;
        }

        return new String(bytes);
    }

    public static boolean hasNetworkConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkCapabilities capabilities = null;
        if (cm != null) {
            capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        }

        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return true;
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return true;
            } else {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
            }
        }
        return false;
    }

    public long getRequestTimestamp() {
        return requestTimestamp;
    }

    public void setCacheExpirationTimeMillis(long cacheExpirationTimeMillis) {
        this.cacheExpirationTimeMillis = cacheExpirationTimeMillis;
    }

    public String readUrl(String urlString, boolean overrideCache) {
        Log.d(TAG, "readUrl()");
        int responseCode = 0;
        String responseText = "";
        long now = System.currentTimeMillis();
        requestTimestamp = 0L;

        File currentCacheFile = getCacheFileForUrl(urlString);
        File currentLockFile = getLockFileForUrl(urlString);

        // load the data from the cache if new enough
        if (!overrideCache && currentCacheFile.exists() && currentCacheFile.lastModified() > now - cacheExpirationTimeMillis) {
            responseText = readFromCacheFile(currentCacheFile);
            requestTimestamp = currentCacheFile.lastModified();
            Log.d(TAG, "Returning from cache for " + urlString);
            return responseText;
        }

        // for unsuccessful attempts we need to block execution for a certain amount of time
        if (currentLockFile.exists() && currentLockFile.lastModified() > now - unsuccessfulAttemptTimeout) {
            Log.i(TAG, "Network access is locked for " + urlString);
            return responseText;
        }

        if (!hasNetworkConnection(context)) {
            Log.i(TAG, "no network connection");
            return responseText;
        }

        createLockFile(currentLockFile);

        Log.i(TAG, "requesting " + urlString);
        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            String userAgent = "NightDream/" + BuildConfig.VERSION_NAME
                    + " (https://github.com/firebirdberlin/NightDream; stefan.fruhner@googlemail.com)";
            Log.i(TAG, "userAgent: " + userAgent);
            urlConnection.setRequestProperty("User-Agent", userAgent);
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);
            responseCode = urlConnection.getResponseCode();
            if (responseCode == 200) {
                responseText = getResponseText(urlConnection.getInputStream());
                requestTimestamp = now;
                storeCacheFile(currentCacheFile, responseText);
            }
            urlConnection.disconnect();
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Http Timeout for " + urlString);
            return responseText;
        } catch (UnknownHostException e) {
            Log.e(TAG, "Unknown host for " + urlString);
            return responseText;
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e), e);
            e.printStackTrace();
        }
        Log.i(TAG, "Returning from network for " + urlString);
        Log.i(TAG, "responseCode: " + responseCode);
        Log.i(TAG, "responseText: " + responseText);
        return responseText;
    }

    private void createLockFile(File lockFileToCreate) {
        long now = System.currentTimeMillis();
        Log.i(TAG, "lockFile: " + lockFileToCreate.getName() + " " + now);
        storeCacheFile(lockFileToCreate, String.valueOf(now));
    }
}