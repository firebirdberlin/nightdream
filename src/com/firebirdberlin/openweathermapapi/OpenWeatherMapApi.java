package com.firebirdberlin.openweathermapapi;


import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.firebirdberlin.nightdream.BuildConfig;
import com.firebirdberlin.openweathermapapi.apimodels.ApiCity;
import com.firebirdberlin.openweathermapapi.apimodels.Clouds;
import com.firebirdberlin.openweathermapapi.apimodels.ListEntry;
import com.firebirdberlin.openweathermapapi.apimodels.Main;
import com.firebirdberlin.openweathermapapi.apimodels.OpenWeatherMapForecastResponse;
import com.firebirdberlin.openweathermapapi.apimodels.Rain;
import com.firebirdberlin.openweathermapapi.apimodels.Snow;
import com.firebirdberlin.openweathermapapi.apimodels.Sys;
import com.firebirdberlin.openweathermapapi.apimodels.Weather;
import com.firebirdberlin.openweathermapapi.apimodels.Wind;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

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

    /**
     * Fetches weather forecast data from OpenWeatherMap API using Gson for JSON parsing.
     * Caching logic is similar to the original method.
     *
     * @param context The application context.
     * @param city    The City object (your existing model) for which to fetch the forecast.
     * @return A list of WeatherEntry objects, or an empty list if data fetching fails.
     */
    static List<WeatherEntry> fetchWeatherForecastApi(
            Context context, com.firebirdberlin.openweathermapapi.models.City city
    ) {
        List<WeatherEntry> forecast = new ArrayList<>();

        if (city == null) {
            return forecast;
        }

        String cityID = (city.id > 0) ? String.format("%d", city.id) : null;
        float lat = (float) city.lat;
        float lon = (float) city.lon;

        String cacheFileName =
                (cityID != null)
                        ? String.format("%s_%s.txt", CACHE_FILE_FORECAST, cityID)
                        : String.format("%s_%3.2f_%3.2f.txt", CACHE_FILE_FORECAST, lat, lon);
        File cacheFile = new File(context.getCacheDir(), cacheFileName);
        Log.d(TAG, "fetchWeatherForecastApi Cache file: " + cacheFileName);

        String responseText = "";
        long requestTimestamp = System.currentTimeMillis();

        // --- Cache Reading Logic ---
        if (cacheFile.exists() && cacheFile.lastModified() > requestTimestamp - CACHE_VALIDITY_TIME) {
            responseText = readFromCacheFile(cacheFile);
            requestTimestamp = cacheFile.lastModified(); // Use cache file's timestamp
            Log.i(TAG, "fetchWeatherForecastApi: Using cached response. Modified: " + requestTimestamp);
        } else {
            // --- Network Request Logic ---
            int responseCode = 0;
            String responseMessage = ""; // Renamed from 'response' to avoid conflict with 'responseText'
            try {
                URL url = getUrlForecast(cityID, lat, lon);
                Log.i(TAG, "fetchWeatherForecastApi: requesting " + url.toString());
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(CONNECT_TIMEOUT); // Set timeouts
                urlConnection.setReadTimeout(READ_TIMEOUT);      // Set timeouts

                responseMessage = urlConnection.getResponseMessage();
                responseCode = urlConnection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    responseText = getResponseText(urlConnection);
                    storeCacheFile(cacheFile, responseText); // Store successful response to cache
                } else {
                    Log.w(TAG, "fetchWeatherForecastApi: API responseCode " + responseCode + " - " + responseMessage);
                }
                urlConnection.disconnect();

            } catch (SocketTimeoutException e) {
                Log.e(TAG, "fetchWeatherForecastApi: Http Timeout", e);
                // Try to read from cache if network fails, even if expired
                if (cacheFile.exists()) {
                    responseText = readFromCacheFile(cacheFile);
                    requestTimestamp = cacheFile.lastModified();
                    Log.i(TAG, "fetchWeatherForecastApi: Network timeout, attempting to use expired cache.");
                } else {
                    return forecast; // No network, no cache
                }
            } catch (Exception e) {
                Log.e(TAG, "fetchWeatherForecastApi: Error fetching data", e);
                // Try to read from cache if network fails, even if expired
                if (cacheFile.exists()) {
                    responseText = readFromCacheFile(cacheFile);
                    requestTimestamp = cacheFile.lastModified();
                    Log.i(TAG, "fetchWeatherForecastApi: Network error, attempting to use expired cache.");
                } else {
                    return forecast; // No network, no cache
                }
            }
        }

        // --- Gson Parsing Logic ---
        if (responseText == null || responseText.isEmpty()) {
            Log.w(TAG, "fetchWeatherForecastApi: No response text found (empty or null).");
            return forecast;
        }

        Gson gson = new Gson();
        OpenWeatherMapForecastResponse apiResponse;
        try {
            apiResponse = gson.fromJson(responseText, OpenWeatherMapForecastResponse.class);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "fetchWeatherForecastApi: Error parsing JSON with Gson", e);
            // Optionally delete corrupt cache file here: cacheFile.delete();
            return forecast;
        }

        if (apiResponse == null || apiResponse.getList() == null || apiResponse.getCity() == null) {
            Log.w(TAG, "fetchWeatherForecastApi: API response or list/city object is null after Gson parsing.");
            return forecast;
        }

        // --- Conversion to WeatherEntry List ---
        ApiCity apiCity = apiResponse.getCity(); // Get the API's city model

        for (ListEntry apiListEntry : apiResponse.getList()) {
            WeatherEntry entry = new WeatherEntry();

            // City information (from the top-level city object in API response)
            entry.cityID = apiCity.getId();
            entry.cityName = apiCity.getName();
            if (apiCity.getCoord() != null) {
                entry.lat = (float) apiCity.getCoord().getLat();
                entry.lon = (float) apiCity.getCoord().getLon();
            } else {
                entry.lat = lat; // Fallback to input lat/lon if city coord is missing
                entry.lon = lon; // Fallback to input lat/lon if city coord is missing
            }

            // Forecast entry specific data
            Main apiMain = apiListEntry.getMain();
            if (apiMain != null) {
                entry.temperature = apiMain.getTemp();
                entry.apparentTemperature = apiMain.getFeelsLike();
                entry.humidity = apiMain.getHumidity();
                // temp_min, temp_max, pressure, sea_level, grnd_level, temp_kf are not in WeatherEntry
            }

            Clouds apiClouds = apiListEntry.getClouds();
            if (apiClouds != null) {
                entry.clouds = apiClouds.getAll();
            }

            Wind apiWind = apiListEntry.getWind();
            if (apiWind != null) {
                entry.windSpeed = apiWind.getSpeed();
                entry.windDirection = apiWind.getDeg();
                // wind.gust is not in WeatherEntry
            }

            Rain apiRain = apiListEntry.getRain();
            if (apiRain != null) {
                entry.rain3h = apiRain.getVolume3h();
                entry.rain1h = -1.0; // Forecast only provides 3h, so set 1h to default/unknown
            } else {
                entry.rain3h = -1.0;
                entry.rain1h = -1.0;
            }

            Snow apiSnow = apiListEntry.getSnow(); // Snow data, if available
            if (apiSnow != null) {
                // WeatherEntry does not have a specific snow volume field, you might need to add one
                // or decide how to represent snow. For now, it's ignored or can be logged.
            }

            List<Weather> apiWeatherList = apiListEntry.getWeather();
            if (apiWeatherList != null && !apiWeatherList.isEmpty()) {
                Weather firstWeather = apiWeatherList.get(0);
                entry.weatherIcon = firstWeather.getIcon();
                entry.description = firstWeather.getDescription();
                entry.weatherIconMeteoconsSymbol = iconToText(firstWeather.getIcon());
            }

            Sys apiSys = apiListEntry.getSys();
            if (apiSys != null) {
                // list.sys.pod is part of the day, not directly mapped to WeatherEntry for now
            }

            entry.timestamp = apiListEntry.getDt();
            entry.request_timestamp = requestTimestamp; // From when the data was requested/cached

            // Sunrise/Sunset times are per city, not per forecast entry,
            // so they are the same for all entries in the forecast list
            entry.sunriseTime = apiCity.getSunrise();
            entry.sunsetTime = apiCity.getSunset();

            // Visibility (int) and POP (double) are available in ListEntry, but not in WeatherEntry
            // if you need them, you might extend WeatherEntry.

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

        Log.i(TAG, "requesting " + url);
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
        if (!lang.isEmpty()) {
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
        if (!lang.isEmpty()) {
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
