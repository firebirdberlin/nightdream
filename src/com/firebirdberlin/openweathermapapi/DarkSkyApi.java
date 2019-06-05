package com.firebirdberlin.openweathermapapi;


import android.net.Uri;
import android.util.Log;

import com.firebirdberlin.nightdream.BuildConfig;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

public class DarkSkyApi {

    public static final String ACTION_WEATHER_DATA_UPDATED = "com.firebirdberlin.nightdream.WEATHER_DATA_UPDATED";
    private static final String ENDPOINT = "https://api.darksky.net/";
    private static String TAG = "DarkSkyApi";
    private static String API_KEY = BuildConfig.API_KEY_DARK_SKY;
    private static int READ_TIMEOUT = 10000;
    private static int CONNECT_TIMEOUT = 10000;

    public static WeatherEntry fetchWeatherData(float lat, float lon) {
        int responseCode = 0;
        String response = "";
        String responseText = "";

        try {
            URL url = getUrlForecast(lat, lon);
            Log.i(TAG, "requesting " + url.toString());
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Accept-Encoding", "gzip");
            response = urlConnection.getResponseMessage();
            responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                responseText = getResponseText(urlConnection);
            }
            urlConnection.disconnect();
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            e.printStackTrace();
        }

        Log.i(TAG, " >> response " + response);
        if (responseCode != HttpURLConnection.HTTP_OK) {
            Log.w(TAG, " >> responseCode " + String.valueOf(responseCode));
            return new WeatherEntry();
        } else {
            Log.i(TAG, " >> responseText " + responseText);
        }

        JSONObject json = getJSONObject(responseText);
        return getWeatherEntryFromJSONObject(json);
    }

    private static double toKelvin(double celsius) {
        return celsius + 273.15;
    }

    private static WeatherEntry getWeatherEntryFromJSONObject(JSONObject json) {
        WeatherEntry entry = new WeatherEntry();
        if (json == null) {
            return entry;
        }

        entry.lat = getValue(json, "latitude", -1.f);
        entry.lon = getValue(json, "longitude", -1.f);

        JSONObject jsonCurrently = getJSONObject(json, "currently");

        entry.cityID = -1;
        entry.cityName = String.format("%3.2f, %3.2f", entry.lat, entry.lon);
        entry.description = getValue(jsonCurrently, "summary", "");
        entry.clouds = (int) (100 * getValue(jsonCurrently, "cloudCover", 0.f));
        entry.timestamp = getValue(jsonCurrently, "time", 0L);
        entry.request_timestamp = System.currentTimeMillis();
        entry.temperature = toKelvin(getValue(jsonCurrently, "temperature", 0.));
        entry.rain3h = -1f;
        entry.sunriseTime = 0L;
        entry.sunsetTime = 0L;

        entry.windSpeed = getValue(jsonCurrently, "windSpeed", 0.);
        entry.windDirection = getValue(jsonCurrently, "windBearing", -1);

        entry.weatherIcon = getValue(jsonCurrently, "icon", "");

        return entry;
    }

    private static JSONObject getJSONObject(String string_representation) {
        try {
            return new JSONObject(string_representation);
        } catch (JSONException e) {
            return null;
        }
    }

    private static JSONObject getJSONObject(JSONArray jsonArray, int index) {
        try {
            return jsonArray.getJSONObject(index);
        } catch (JSONException e) {
            return null;
        }
    }

    private static JSONObject getJSONObject(JSONObject json, String name) {
        try {
            return json.getJSONObject(name);
        } catch (JSONException e) {
            return null;
        }
    }

    private static JSONArray getJSONArray(JSONObject json, String name) {
        try {
            return json.getJSONArray(name);
        } catch (JSONException e) {
            return null;
        }
    }

    private static double getValue(JSONObject json, String name, double defaultvalue) {
        if (json == null) return defaultvalue;
        try {
            return json.getDouble(name);
        } catch (JSONException e) {
            return defaultvalue;
        }
    }

    private static float getValue(JSONObject json, String name, float defaultvalue) {
        return (float) getValue(json, name, (double) defaultvalue);
    }

    private static int getValue(JSONObject json, String name, int defaultvalue) {
        if (json == null) return defaultvalue;
        try {
            return json.getInt(name);
        } catch (JSONException e) {
            return defaultvalue;
        }
    }

    private static String getValue(JSONObject json, String name, String defaultvalue) {
        if (json == null) return defaultvalue;
        try {
            return json.getString(name);
        } catch (JSONException e) {
            return defaultvalue;
        }
    }

    private static long getValue(JSONObject json, String name, long defaultvalue) {
        if (json == null) return defaultvalue;
        try {
            return json.getLong(name);
        } catch (JSONException e) {
            return defaultvalue;
        }
    }

    private static String getResponseText(HttpURLConnection c) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(c.getInputStream())));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    private static URL getUrlForecast(float lat, float lon) throws MalformedURLException {
        Uri.Builder builder = getPathBuilder("forecast");

        builder = builder
                .appendPath(String.valueOf(lat) + "," + String.valueOf(lon))
                .appendQueryParameter("units", "si");

        String lang = getDefaultLanguage();
        if (lang != null && !lang.isEmpty()) {
            builder = builder.appendQueryParameter("lang", lang);
        }

        String url = builder.build().toString();
        return new URL(url);
    }

    private static Uri.Builder getPathBuilder(String endpoint) {
        return Uri.parse(ENDPOINT).buildUpon()
                .appendPath(endpoint)
                .appendPath(API_KEY);
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

    private static String getDefaultLanguage() {
        return Locale.getDefault().getLanguage();
    }
}
