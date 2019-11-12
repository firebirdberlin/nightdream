package com.firebirdberlin.openweathermapapi;


import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.firebirdberlin.nightdream.BuildConfig;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

public class DarkSkyApi {

    public static final String ACTION_WEATHER_DATA_UPDATED = "com.firebirdberlin.nightdream.WEATHER_DATA_UPDATED";
    private static final String ENDPOINT = "https://api.darksky.net/";
    private static String CACHE_FILE = "DarkSkyApi";
    private static String TAG = "DarkSkyApi";
    private static String API_KEY = BuildConfig.API_KEY_DARK_SKY;
    private static int READ_TIMEOUT = 10000;
    private static int CONNECT_TIMEOUT = 10000;
    private static long CACHE_VALIDITY_TIME = 1000 * 60 * 60; // 60 mins

    public static WeatherEntry fetchCurrentWeatherData(Context context, City city, float lat, float lon) {

        String responseText = fetchWeatherData(context, city, lat, lon);
        if (responseText == null) {
            return new WeatherEntry();
        }
        JSONObject json = getJSONObject(responseText);
        return getWeatherEntryFromJSONObject(city, json);
    }

    public static List<WeatherEntry> fetchHourlyWeatherData(Context context, City city) {

        String responseText = fetchWeatherData(context, city, (float) city.lat, (float) city.lon);
        if (responseText == null) {
            return new ArrayList<>();
        }
        JSONObject json = getJSONObject(responseText);
        return getWeatherEntriesHourly(city, json);
    }

    private static String fetchWeatherData(Context context, City city, float lat, float lon) {
        int responseCode = 0;
        String response = "";
        String responseText = "";

        String cacheFileName =
                (city != null)
                        ? String.format("%s_%d.txt", CACHE_FILE, city.id)
                        : String.format("%s_%3.2f_%3.2f.txt", CACHE_FILE, lat, lon);
        File cacheFile = new File(context.getCacheDir(), cacheFileName);
        long now = System.currentTimeMillis();
        if (cacheFile.exists()) {
            Log.i(TAG, "Cache file modify time: " + cacheFile.lastModified() );
            Log.i(TAG, "new enough: " + (cacheFile.lastModified() > now - CACHE_VALIDITY_TIME));
        }
        if (cacheFile.exists() && cacheFile.lastModified() > now - CACHE_VALIDITY_TIME ) {
            responseText = readFromCacheFile(cacheFile);
        } else {

            try {
                URL url = getUrlForecast(city, lat, lon);
                Log.i(TAG, "requesting " + url.toString());
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setReadTimeout(READ_TIMEOUT);
                urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
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
                Log.w(TAG, " >> responseCode " + responseCode);
                return null;
            }
            storeCacheFile(cacheFile, responseText);
        }
        Log.i(TAG, " >> responseText " + responseText);
        return responseText;
    }

    private static void storeCacheFile(File cacheFile, String responseText) {
        try {
            FileOutputStream stream = new FileOutputStream(cacheFile);
            stream.write(responseText.getBytes());
            stream.close();
        } catch (IOException e) {
            /*
            if (cacheFile.exists()) {
                cacheFile.delete();
            }
            */
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

    private static double toKelvin(double celsius) {
        return celsius + 273.15;
    }

    private static WeatherEntry getWeatherEntryFromJSONObject(City city, JSONObject json) {
        WeatherEntry entry = new WeatherEntry();
        if (json == null) {
            return entry;
        }

        entry.lat = getValue(json, "latitude", -1.f);
        entry.lon = getValue(json, "longitude", -1.f);

        JSONObject jsonCurrently = getJSONObject(json, "currently");


        entry.cityID = (city != null) ? city.id : -1;
        entry.cityName = (city != null) ? city.name : String.format("%3.2f, %3.2f", entry.lat, entry.lon);
        entry.clouds = (int) (100 * getValue(jsonCurrently, "cloudCover", 0.f));
        entry.description = getValue(jsonCurrently, "summary", "");
        entry.rain1h = getValue(jsonCurrently, "precipIntensity", -1.);
        entry.rain3h = -1f;
        entry.request_timestamp = System.currentTimeMillis();
        entry.sunriseTime = 0L;
        entry.sunsetTime = 0L;
        entry.temperature = toKelvin(getValue(jsonCurrently, "temperature", 0.));
        entry.timestamp = getValue(jsonCurrently, "time", 0L);
        entry.weatherIcon = getValue(jsonCurrently, "icon", "");
        entry.windDirection = getValue(jsonCurrently, "windBearing", -1);
        entry.windSpeed = getValue(jsonCurrently, "windSpeed", 0.);

        return entry;
    }

    private static List<WeatherEntry> getWeatherEntriesHourly(City city, JSONObject json) {
        List<WeatherEntry> entries = new ArrayList<>();
        if (json == null) {
            return entries;
        }
        long now = System.currentTimeMillis();
        float lat = getValue(json, "latitude", -1.f);
        float lon = getValue(json, "longitude", -1.f);
        JSONObject jsonHourly = getJSONObject(json, "hourly");
        JSONArray jsonData = getJSONArray(jsonHourly, "data");

        for(int i=0; i< jsonData.length(); i++){
            JSONObject jsonEntry = getJSONObject(jsonData, i);
            WeatherEntry entry = new WeatherEntry();
            entry.cityID = (city != null) ? city.id : -1;
            entry.cityName = (city != null) ? city.name : String.format("%3.2f, %3.2f", entry.lat, entry.lon);
            entry.clouds = (int) (100 * getValue(jsonEntry, "cloudCover", 0.f));
            entry.description = getValue(jsonEntry, "summary", "");
            entry.lat = lat;
            entry.lon = lon;
            entry.rain1h = getValue(jsonEntry, "precipIntensity", -1.);
            entry.rain3h = -1f;
            entry.request_timestamp = now;
            entry.sunriseTime = 0L;
            entry.sunsetTime = 0L;
            entry.temperature = toKelvin(getValue(jsonEntry, "temperature", 0.));
            entry.timestamp = getValue(jsonEntry, "time", 0L);
            entry.weatherIcon = getValue(jsonEntry, "icon", "");
            entry.windDirection = getValue(jsonEntry, "windBearing", -1);
            entry.windSpeed = getValue(jsonEntry, "windSpeed", 0.);
            entries.add(entry);
        }
        return entries;
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

    private static URL getUrlForecast(City city, float lat, float lon) throws MalformedURLException {
        Uri.Builder builder = getPathBuilder("forecast");

        if (city != null) {
            lat = (float) city.lat;
            lon = (float) city.lon;
        }

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
