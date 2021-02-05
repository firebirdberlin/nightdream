package com.firebirdberlin.dwd;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

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
    private final File cacheFile;
    private final File lockFile;
    private final Context context;

    private final long cacheExpirationTimeMillis = 1000 * 60 * 60 * 24;
    private final long unsuccessfulAttemptTimeout = 1000 * 60 * 10; // 10 Minutes

    public HttpReader(Context context, final String cacheFileName) {
        cacheFile = new File(context.getCacheDir(), cacheFileName);
        lockFile = new File(context.getCacheDir(), cacheFileName + ".lock~");
        this.context = context;
    }

    private static String getResponseText(InputStream inputStream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
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

    public String readUrl(String urlString, boolean overrideCache) {
        Log.d(TAG, "readUrl()");
        int responseCode = 0;
        String responseText = "";
        long now = System.currentTimeMillis();

        // load the data from the cache if new enough
        if (!overrideCache && cacheFile.exists() && cacheFile.lastModified() > now - cacheExpirationTimeMillis) {
            responseText = readFromCacheFile(cacheFile);
            Log.d(TAG, "Returning from cache");
            return responseText;
        }

        // for unsuccessful attempts we need to block execution for a certain amount of time
        if (lockFile.exists() && lockFile.lastModified() > now - unsuccessfulAttemptTimeout) {
            Log.i(TAG, "Network access is locked");
            return responseText;
        }

        if (!hasNetworkConnection(context)) {
            Log.i(TAG, "no network connection");
            return responseText;
        }

        createLockFile();

        Log.i(TAG, "requesting " + urlString);
        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);
            responseCode = urlConnection.getResponseCode();
            if (responseCode == 200) {
                responseText = getResponseText(urlConnection.getInputStream());
                storeCacheFile(cacheFile, responseText);
            }
            urlConnection.disconnect();
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Http Timeout");
            return responseText;
        } catch (UnknownHostException e) {
            Log.e(TAG, "Unknown host");
            return responseText;
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e), e);
            e.printStackTrace();
        }

        return responseText;
    }

    private void createLockFile() {
        long now = System.currentTimeMillis();
        Log.i(TAG, "lockFile: " + now);
        storeCacheFile(lockFile, String.valueOf(now));
    }

    public static boolean hasNetworkConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        } else {
            NetworkInfo activeNetwork = null;
            if (cm != null) {
                activeNetwork = cm.getActiveNetworkInfo();
            }
            return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
        }
        return false;
    }
}
