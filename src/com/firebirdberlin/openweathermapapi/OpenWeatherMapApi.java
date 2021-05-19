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
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OpenWeatherMapApi {

    public static final String ACTION_WEATHER_DATA_UPDATED = "com.firebirdberlin.nightdream.WEATHER_DATA_UPDATED";
    private static final String ENDPOINT = "https://api.openweathermap.org/data/2.5";
    private static String TAG = "OpenWeatherMapApi";
    private static String APPID = BuildConfig.API_KEY_OWM;
    private static int READ_TIMEOUT = 60000;
    private static int CONNECT_TIMEOUT = 60000;
    private static String CACHE_FILE_FORECAST = "owm_forecast";
    private static String CACHE_FILE_DATA = "owm_weather_data";
    private static long CACHE_VALIDITY_TIME = 1000 * 60 * 60; // 60 mins

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

    public static WeatherEntry fetchWeatherData(
            Context context, String cityID, float lat, float lon
    ) {
        int responseCode = 0;
        String response = "";
        String responseText = "";

        if (cityID != null && !cityID.isEmpty()) {
            if (Integer.parseInt(cityID) < 0) {
                cityID = null;
            }
        }

        String cacheFileName = "weather_unknown.txt";
        try {
            cacheFileName =
                    (cityID != null && !cityID.isEmpty())
                            ? String.format("%s_%s.txt", CACHE_FILE_DATA, cityID)
                            : String.format("%s_%3.2f_%3.2f.txt", CACHE_FILE_DATA, lat, lon);
        } catch (NumberFormatException ignored) {}
        Log.d(TAG, cacheFileName);
        File cacheFile = new File(context.getCacheDir(), cacheFileName);
        long now = System.currentTimeMillis();
        if (cacheFile.exists()) {
            Log.i(TAG, "Cache file modify time: " + cacheFile.lastModified());
            Log.i(TAG, "new enough: " + (cacheFile.lastModified() > now - CACHE_VALIDITY_TIME));
        }

        long requestTimestamp = now;
        if (cacheFile.exists() && cacheFile.lastModified() > now - CACHE_VALIDITY_TIME) {
            responseText = readFromCacheFile(cacheFile);
            requestTimestamp = cacheFile.lastModified();
        } else {
            try {
                URL url = getUrlWeather(cityID, lat, lon);
                Log.i(TAG, "requesting " + url.toString());
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
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
            storeCacheFile(cacheFile, responseText);
            Log.i(TAG, " >> response " + response);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, " >> responseCode " + responseCode);
                return null;
            }
        }

        Log.i(TAG, " >> responseText " + responseText);

        JSONObject json = getJSONObject(responseText);
        return getWeatherEntryFromJSONObject(json, requestTimestamp);
    }

    static List<WeatherEntry> fetchWeatherForecast(
            Context context, City city
    ) {
        int responseCode = 0;
        String response = "";
        String responseText = "";

        if (city == null) {
            return null;
        }
        List<WeatherEntry> forecast = new ArrayList<WeatherEntry>();

        String cityID = (city.id > 0) ? String.format("%d", city.id) : null;
        float lat = (float) city.lat;
        float lon = (float) city.lon;
        String cacheFileName =
                (cityID != null)
                        ? String.format("%s_%s.txt", CACHE_FILE_FORECAST, cityID)
                        : String.format("%s_%3.2f_%3.2f.txt", CACHE_FILE_FORECAST, lat, lon);
        File cacheFile = new File(context.getCacheDir(), cacheFileName);
        Log.d(TAG, cacheFileName);
        long now = System.currentTimeMillis();
        if (cacheFile.exists()) {
            Log.i(TAG, "Cache file modify time: " + cacheFile.lastModified());
            Log.i(TAG, "new enough: " + (cacheFile.lastModified() > now - CACHE_VALIDITY_TIME));
        }
        long requestTimestamp = now;
        if (cacheFile.exists() && cacheFile.lastModified() > now - CACHE_VALIDITY_TIME) {
            responseText = readFromCacheFile(cacheFile);
            requestTimestamp = cacheFile.lastModified();
            Log.i(TAG, "response: " + responseText);
        } else {
            try {
                URL url = getUrlForecast(cityID, lat, lon);
                Log.i(TAG, "requesting " + url.toString());
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                response = urlConnection.getResponseMessage();
                responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    responseText = getResponseText(urlConnection);
                }
                urlConnection.disconnect();
                storeCacheFile(cacheFile, responseText);

                Log.i(TAG, " >> response " + response);
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, " >> responseCode " + responseCode);
                    return forecast;
                }
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
                e.printStackTrace();
            }

            Log.i(TAG, " >> responseText " + responseText);
        }

        if (responseText == null || responseText.isEmpty()) {
            return forecast;
        }

        JSONObject json = getJSONObject(responseText);
        JSONObject jsonCity = getJSONObject(json, "city");
        String cityName = getValue(jsonCity, "name", "");
        int cityIDint = getValue(jsonCity, "id", 0);
        JSONArray jsonList = getJSONArray(json, "list");
        if (jsonList == null) {
            return forecast;
        }
        for (int i = 0; i < jsonList.length(); i++) {
            JSONObject jsonEntry = getJSONObject(jsonList, i);
            WeatherEntry entry = getWeatherEntryFromJSONObject(jsonEntry, requestTimestamp);
            entry.cityID = cityIDint;
            entry.cityName = cityName;
            forecast.add(entry);
        }
        return forecast;
    }

    private static WeatherEntry getWeatherEntryFromJSONObject(
            JSONObject json, long requestTimestamp
    ) {
        if (json == null) {
            return null;
        }
        JSONObject jsonMain = getJSONObject(json, "main");
        JSONObject jsonCoord = getJSONObject(json, "coord");
        JSONObject jsonClouds = getJSONObject(json, "clouds");
        JSONObject jsonRain = getJSONObject(json, "rain");
        JSONObject jsonWind = getJSONObject(json, "wind");
        JSONObject jsonSys = getJSONObject(json, "sys");
        JSONArray jsonWeather = getJSONArray(json, "weather");

        WeatherEntry entry = new WeatherEntry();
        entry.cityID = getValue(json, "id", 0);
        entry.lat = getValue(jsonCoord, "lat", -1.f);
        entry.lon = getValue(jsonCoord, "lon", -1.f);
        entry.cityName = getValue(json, "name", "");
        entry.clouds = getValue(jsonClouds, "all", 0);
        entry.timestamp = getValue(json, "dt", 0L);
        entry.request_timestamp = requestTimestamp;
        entry.humidity = getValue(jsonMain, "humidity", -1);
        entry.temperature = getValue(jsonMain, "temp", 0.);
        entry.apparentTemperature = getValue(jsonMain, "feels_like", entry.temperature);
        entry.rain1h = getValue(jsonRain, "1h", -1.);
        entry.rain3h = getValue(jsonRain, "3h", -1.);
        entry.sunriseTime = getValue(jsonSys, "sunrise", 0L);
        entry.sunsetTime = getValue(jsonSys, "sunset", 0L);

        entry.windSpeed = getValue(jsonWind, "speed", 0.);
        entry.windDirection = getValue(jsonWind, "deg", -1);

        entry.weatherIcon = "";
        if (jsonWeather != null && jsonWeather.length() > 0) {
            JSONObject weatherObj = getJSONObject(jsonWeather, 0);
            entry.weatherIcon = getValue(weatherObj, "icon", "");
            entry.description = getValue(weatherObj, "description", "");
            entry.weatherIconMeteoconsSymbol = iconToText(entry.weatherIcon);
        }

        return entry;
    }

    private static String iconToText(String code) {
        if (code == null) return "";
        // openweathermap
        if (code.equals("01d")) return "B";
        if (code.equals("01n")) return "C";
        if (code.equals("02d")) return "H";
        if (code.equals("02n")) return "I";
        if (code.equals("03d")) return "N";
        if (code.equals("03n")) return "N";
        if (code.equals("04d")) return "Y";
        if (code.equals("04n")) return "Y";
        if (code.equals("09d")) return "R";
        if (code.equals("09n")) return "R";
        if (code.equals("10d")) return "Q";
        if (code.equals("10n")) return "Q";
        if (code.equals("11d")) return "0";
        if (code.equals("11n")) return "0";
        if (code.equals("13d")) return "W";
        if (code.equals("13n")) return "W";
        if (code.equals("50d")) return "M";
        if (code.equals("50n")) return "M";

        return "";
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
        } catch (JSONException | NullPointerException e) {
            return null;
        }
    }

    private static JSONArray getJSONArray(JSONObject json, String name) {
        try {
            return json.getJSONArray(name);
        } catch (JSONException | NullPointerException e) {
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
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    static List<City> findCity(String query) {

        int responseCode = 0;
        String response = "";
        String responseText = "";

        List<City> cities = new ArrayList<>();

        URL url;
        try {
            url = getUrlFindCity(query);
        } catch (MalformedURLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            e.printStackTrace();
            return cities;
        }

        Log.i(TAG, "requesting " + url.toString());
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);
            response = urlConnection.getResponseMessage();
            responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                responseText = getResponseText(urlConnection.getInputStream());
            }
            urlConnection.disconnect();
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Http Timeout");
            return cities;
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            e.printStackTrace();
        }

        Log.i(TAG, " >> response " + response);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            Log.i(TAG, " >> responseText " + responseText);
            try {
                cities = decodeCitiesJsonResponse(responseText);
            } catch (JSONException e) {
                e.printStackTrace();
                //TODO resilience (count errors and if error threshold is reached (API service broken), disable fetchCountries for some time to save api requests)
            }

        } else {
            Log.w(TAG, " >> responseCode " + responseCode);
        }
        return cities;
    }

    private static URL getUrlFindCity(String query) throws MalformedURLException {
        Uri.Builder builder = getPathBuilder("find");
        String url = builder
                .appendQueryParameter("q", query)
                .appendQueryParameter("type", "like")
                .appendQueryParameter("cnt", "15")
                .appendQueryParameter("sort", "population")
                .build().toString();
        return new URL(url);
    }

    private static URL getUrlWeather(String cityId, float lat, float lon) throws MalformedURLException {
        Uri.Builder builder = getPathBuilder("weather");

        if (cityId != null && !cityId.isEmpty()) {
            builder = builder.appendQueryParameter("id", cityId);
        } else {
            builder = builder
                    .appendQueryParameter("lat", String.valueOf(lat))
                    .appendQueryParameter("lon", String.valueOf(lon));
        }

        String lang = getDefaultLanguage();
        if (lang != null && !lang.isEmpty()) {
            builder = builder.appendQueryParameter("lang", lang);
        }

        String url = builder.build().toString();
        return new URL(url);
    }

    private static URL getUrlForecast(String cityId, float lat, float lon) throws MalformedURLException {
        Uri.Builder builder = getPathBuilder("forecast");

        if (cityId != null && !cityId.isEmpty()) {
            builder = builder.appendQueryParameter("id", cityId);
        } else {
            builder = builder
                    .appendQueryParameter("lat", String.valueOf(lat))
                    .appendQueryParameter("lon", String.valueOf(lon));
        }
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
                .appendQueryParameter("appid", APPID);
    }


    private static List<City> decodeCitiesJsonResponse(String responseText) throws JSONException {
        List<City> cities = new ArrayList<>();

        JSONObject json = new JSONObject(responseText);
        JSONArray jsonArray = json.getJSONArray("list");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonCity = jsonArray.getJSONObject(i);
            JSONObject jsonSys = jsonCity.getJSONObject("sys");
            JSONObject jsonCoord = jsonCity.getJSONObject("coord");

            City city = new City();
            city.id = jsonCity.getInt("id");
            city.name = jsonCity.getString("name");
            city.countryCode = jsonSys.getString("country");
            city.lat = jsonCoord.getDouble("lat");
            city.lon = jsonCoord.getDouble("lon");

            cities.add(city);
        }

        return cities;
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
