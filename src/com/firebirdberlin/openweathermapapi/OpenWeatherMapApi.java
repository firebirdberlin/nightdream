package com.firebirdberlin.openweathermapapi;


import android.net.Uri;
import android.util.Log;

import com.firebirdberlin.openweathermapapi.models.City;
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
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class OpenWeatherMapApi {

    public static final String ACTION_WEATHER_DATA_UPDATED = "com.firebirdberlin.nightdream.WEATHER_DATA_UPDATED";
    private static final String ENDPOINT = "http://api.openweathermap.org/data/2.5";
    private static String TAG = "OpenWeatherMapApi";
    private static String APPID = "645d3eb40425e8af8edc25ddbf153db8";
    private static int READ_TIMEOUT = 10000;
    private static int CONNECT_TIMEOUT = 10000;

    public static WeatherEntry fetchWeatherData(String cityID, float lat, float lon) {
        int responseCode = 0;
        String response = "";
        String responseText = "";

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

    public static List<WeatherEntry> fetchWeatherForecast(String cityID, float lat, float lon) {
        int responseCode = 0;
        String response = "";
        String responseText = "";

        List<WeatherEntry> forecast = new ArrayList<WeatherEntry>();

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
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            e.printStackTrace();
        }

        Log.i(TAG, " >> response " + response);
        if (responseCode != HttpURLConnection.HTTP_OK) {
            Log.w(TAG, " >> responseCode " + String.valueOf(responseCode));
            return forecast;
        } else {
            Log.i(TAG, " >> responseText " + responseText);
        }
        JSONObject json = getJSONObject(responseText);
        JSONObject jsonCity = getJSONObject(json, "city");
        String cityName = getValue(jsonCity, "name", "");
        int cityIDint = getValue(jsonCity, "id", 0);
        JSONArray jsonList = getJSONArray(json, "list");
        for(int i=0; i< jsonList.length(); i++){
            JSONObject jsonEntry = getJSONObject(jsonList, i);
            WeatherEntry entry = getWeatherEntryFromJSONObject(jsonEntry);
            entry.cityID = cityIDint;
            entry.cityName = cityName;
            forecast.add(entry);
        }
        return forecast;
    }

    private static WeatherEntry getWeatherEntryFromJSONObject(JSONObject json) {
        WeatherEntry entry = new WeatherEntry();
        JSONObject jsonMain = getJSONObject(json, "main");
        JSONObject jsonWind = getJSONObject(json, "wind");
        JSONObject jsonSys = getJSONObject(json, "sys");
        JSONArray jsonWeather = getJSONArray(json, "weather");

        entry.cityID = getValue(json, "id", 0);
        entry.cityName = getValue(json, "name", "");
        entry.timestamp = getValue(json, "dt", 0L);
        entry.temperature = getValue(jsonMain, "temp", 0.);
        entry.sunriseTime = getValue(jsonSys, "sunrise", 0L);
        entry.sunsetTime = getValue(jsonSys, "sunset", 0L);

        entry.windSpeed = getValue(jsonWind, "speed", 0.);
        entry.windDirection = getValue(jsonWind, "deg", -1);

        entry.weatherIcon = "";
        if (jsonWeather != null && jsonWeather.length() > 0) {
            JSONObject weatherObj = getJSONObject(jsonWeather, 0);
            entry.weatherIcon = getValue(weatherObj, "icon", "");
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
            if ( responseCode == HttpURLConnection.HTTP_OK ) {
                responseText = getResponseText(urlConnection.getInputStream());
            }
            urlConnection.disconnect();
        }
        catch (SocketTimeoutException e) {
            Log.e(TAG, "Http Timeout");
            return cities;
        }
        catch (Exception e) {
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
            Log.w(TAG, " >> responseCode " + String.valueOf(responseCode));
        }
        return cities;
    }

    private static URL getUrlFindCity(String query) throws MalformedURLException {
        query = query.replace(" ", "");
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

        if (!cityId.isEmpty()) {
            builder = builder.appendQueryParameter("id", cityId);
        } else {
            builder = builder
                    .appendQueryParameter("lat", String.valueOf(lat))
                    .appendQueryParameter("lon", String.valueOf(lon));
        }

        String url = builder.build().toString();
        return new URL(url);
    }

    private static URL getUrlForecast(String cityId, float lat, float lon) throws MalformedURLException {
        Uri.Builder builder = getPathBuilder("forecast");

        if (!cityId.isEmpty()) {
            builder = builder.appendQueryParameter("id", cityId);
        } else {
            builder = builder
                    .appendQueryParameter("lat", String.valueOf(lat))
                    .appendQueryParameter("lon", String.valueOf(lon));
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
}
