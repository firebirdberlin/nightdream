package com.firebirdberlin.openweathermapapi;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.firebirdberlin.HttpReader;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BrightSkyApi {
    public static final String ACTION_WEATHER_DATA_UPDATED = "com.firebirdberlin.nightdream.WEATHER_DATA_UPDATED";
    private static final String ENDPOINT = "https://api.brightsky.dev";
    private static String CACHE_FILE = "BrightSkyApi";
    private static String TAG = "BrightSkyApi";

    public static WeatherEntry fetchCurrentWeatherData(Context context, City city, float lat, float lon) {
        String responseText = fetchWeatherData(context, city, lat, lon);
        if (responseText == null) {
            return new WeatherEntry();
        }
        JSONObject json = getJSONObject(responseText);
        return getWeatherEntryFromJSONObject(city, json, context);
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
        Log.d(TAG, "fetchWeatherData");
        String responseText = "";

        if (city != null) {
            lat = (float) city.lat;
            lon = (float) city.lon;
        }
        String cacheFileName = String.format(
                java.util.Locale.getDefault(), "%s_%3.2f_%3.2f.txt", CACHE_FILE, lat, lon
        );

        HttpReader httpReader = new HttpReader(context, cacheFileName);
        httpReader.setCacheExpirationTimeMillis(1000 * 60 * 60); // 1 hour
        URL url;
        try {
            url = getUrlForecast(lat, lon);
            responseText = httpReader.readUrl(url.toString(), false);
            return responseText;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static double toKelvin(double celsius) {
        return celsius + 273.15;
    }

    private static double toMetersPerSecond(double kilometersPerHour) {
        return kilometersPerHour * 1000./3600.;
    }

    //current weather
    private static WeatherEntry getWeatherEntryFromJSONObject(City city, JSONObject json, Context mContext) {
        Log.d(TAG, "getWeatherEntryFromJSONObject");
        WeatherEntry entry = new WeatherEntry();
        if (json == null) {
            return entry;
        }
        long now = System.currentTimeMillis();
        float lat = getValue(json, "latitude", -1.f);
        float lon = getValue(json, "longitude", -1.f);

        JSONArray jsonData = getJSONArray(json, "weather");
        JSONObject jsonEntry;

        if (jsonData != null) {
            for (int i = 0; i < jsonData.length(); i++) {
                jsonEntry = getJSONObject(jsonData, i);
                WeatherEntry new_entry = parseJsonObject(jsonEntry, city, lat, lon, now);

                if (new_entry.timestamp > now) {
                    return entry;
                }
                entry = new_entry;
            }

        }
        return entry;
    }

    //forecast weather
    private static List<WeatherEntry> getWeatherEntriesHourly(City city, JSONObject json) {
        List<WeatherEntry> entries = new ArrayList<>();
        if (json == null) {
            return entries;
        }
        long now = System.currentTimeMillis();
        float lat = getValue(json, "latitude", -1.f);
        float lon = getValue(json, "longitude", -1.f);
        JSONArray jsonData = getJSONArray(json, "weather");

        if (jsonData != null) {
            for (int i = 0; i < jsonData.length(); i++) {
                JSONObject jsonEntry = getJSONObject(jsonData, i);
                WeatherEntry entry = parseJsonObject(jsonEntry, city, lat, lon, now);
                entries.add(entry);
            }
        }

        return entries;
    }

    private static WeatherEntry parseJsonObject(JSONObject jsonEntry, City city, float lat, float lon, long now) {
        WeatherEntry entry = new WeatherEntry();
        entry.cityID = (city != null) ? city.id : -1;
        entry.cityName = (city != null) ? city.name : String.format(java.util.Locale.getDefault(), "%3.2f, %3.2f", entry.lat, entry.lon);
        entry.clouds = (int) (getValue(jsonEntry, "cloud_cover", 0.f));
        entry.description = getValue(jsonEntry, "condition", "");
        entry.lat = lat;
        entry.lon = lon;
        entry.rain1h = getValue(jsonEntry, "precipitation", -1.);
        entry.rain3h = -1f;
        entry.request_timestamp = now;
        entry.sunriseTime = 0L;
        entry.sunsetTime = 0L;
        entry.temperature = toKelvin(getValue(jsonEntry, "temperature", -273.15));
        entry.apparentTemperature = -273.15;
        entry.humidity = getValue(jsonEntry, "relative_humidity", -1);
        entry.weatherIcon = getValue(jsonEntry, "icon", "");
        entry.windDirection = getValue(jsonEntry, "wind_direction", -1);
        entry.windSpeed = toMetersPerSecond(getValue(jsonEntry, "wind_speed", 0.));

        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ", java.util.Locale.getDefault());
            Date result = df.parse(String.valueOf(getValue(jsonEntry, "timestamp", "")));
            entry.timestamp = result.getTime() / 1000;
        } catch (ParseException e) {
            e.printStackTrace();
            entry.timestamp = -1L;
        }

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

    private static URL getUrlForecast(float lat, float lon) throws MalformedURLException {
        Calendar now = Calendar.getInstance();
        Date todayDate = now.getTime();
        now.add(Calendar.DAY_OF_YEAR, 7);
        Date lastDate = now.getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String todayString = formatter.format(todayDate);
        String lastDateString = formatter.format(lastDate);

        Uri.Builder builder = Uri
                .parse(ENDPOINT)
                .buildUpon()
                .appendPath("weather")
                .appendQueryParameter("lat", String.valueOf(lat))
                .appendQueryParameter("lon", String.valueOf(lon))
                .appendQueryParameter("units", "dwd")
                .appendQueryParameter("date", todayString)
                .appendQueryParameter("last_date", lastDateString);

        String url = builder.build().toString();
        return new URL(url);
    }
}