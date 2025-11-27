package com.firebirdberlin.openweathermapapi;


import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.firebirdberlin.nightdream.BuildConfig;
import com.firebirdberlin.openweathermapapi.apimodels.ApiCity;
import com.firebirdberlin.openweathermapapi.apimodels.ApiCoord;
import com.firebirdberlin.openweathermapapi.apimodels.Clouds;
import com.firebirdberlin.openweathermapapi.apimodels.CurrentSys;
import com.firebirdberlin.openweathermapapi.apimodels.FindCityEntry;
import com.firebirdberlin.openweathermapapi.apimodels.ListEntry;
import com.firebirdberlin.openweathermapapi.apimodels.Main;
import com.firebirdberlin.openweathermapapi.apimodels.OpenWeatherMapCurrentResponse;
import com.firebirdberlin.openweathermapapi.apimodels.OpenWeatherMapFindCityResponse;
import com.firebirdberlin.openweathermapapi.apimodels.OpenWeatherMapForecastResponse;
import com.firebirdberlin.openweathermapapi.apimodels.Rain;
import com.firebirdberlin.openweathermapapi.apimodels.Snow;
import com.firebirdberlin.openweathermapapi.apimodels.Sys;
import com.firebirdberlin.openweathermapapi.apimodels.Weather;
import com.firebirdberlin.openweathermapapi.apimodels.Wind;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

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
    private static final String TAG = "OpenWeatherMapApi";
    private static final int READ_TIMEOUT = 60000;
    private static final int CONNECT_TIMEOUT = 60000;
    private static final long CACHE_VALIDITY_TIME = 1000 * 60 * 60; // 60 mins

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

    /**
     * Fetches current weather data from OpenWeatherMap API using Gson for JSON parsing.
     * Caching logic is similar to the original method.
     *
     * @param context The application context.
     * @param cityID  The city ID (optional, if lat/lon are provided).
     * @param lat     Latitude of the location (used if cityID is null or empty).
     * @param lon     Longitude of the location (used if cityID is null or empty).
     * @return A WeatherEntry object, or null if data fetching fails.
     */
    public static WeatherEntry fetchWeatherDataApi(
            Context context, String cityID, float lat, float lon
    ) {
        String responseText = "";
        long requestTimestamp = System.currentTimeMillis();

        if (cityID != null && !cityID.isEmpty()) {
            try {
                if (Integer.parseInt(cityID) < 0) { // Check for invalid cityID, similar to original
                    cityID = null;
                }
            } catch (NumberFormatException ignored) {
                cityID = null; // Treat invalid cityID string as null
            }
        }

        String cacheFileName = "weather_unknown.txt";
        try {
            String CACHE_FILE_DATA = "owm_weather_data";
            cacheFileName =
                    (cityID != null && !cityID.isEmpty())
                            ? String.format("%s_%s.txt", CACHE_FILE_DATA, cityID)
                            : String.format("%s_%3.2f_%3.2f.txt", CACHE_FILE_DATA, lat, lon);
        } catch (Exception ignored) {} // Catch potential formatting errors, keep default name
        File cacheFile = new File(context.getCacheDir(), cacheFileName);
        Log.d(TAG, "fetchWeatherDataApi Cache file: " + cacheFileName);

        // --- Cache Reading Logic ---
        if (cacheFile.exists()) {
            Log.i(TAG, "Cache file modify time: " + cacheFile.lastModified());
            Log.i(TAG, "new enough: " + (cacheFile.lastModified() > requestTimestamp - CACHE_VALIDITY_TIME));
        }

        if (cacheFile.exists() && cacheFile.lastModified() > requestTimestamp - CACHE_VALIDITY_TIME) {
            responseText = readFromCacheFile(cacheFile);
            requestTimestamp = cacheFile.lastModified(); // Use cache file's timestamp
            Log.i(TAG, "fetchWeatherDataApi: Using cached response. Modified: " + requestTimestamp);
        } else {
            // --- Network Request Logic ---
            int responseCode = 0;
            String responseMessage = "";
            try {
                URL url = getUrlWeather(cityID, lat, lon);
                Log.i(TAG, "fetchWeatherDataApi: requesting " + url.toString());
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
                urlConnection.setReadTimeout(READ_TIMEOUT);

                responseMessage = urlConnection.getResponseMessage();
                responseCode = urlConnection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    responseText = getResponseText(urlConnection);
                    storeCacheFile(cacheFile, responseText); // Store successful response to cache
                } else {
                    Log.w(TAG, "fetchWeatherDataApi: API responseCode " + responseCode + " - " + responseMessage);
                }
                urlConnection.disconnect();

            } catch (SocketTimeoutException e) {
                Log.e(TAG, "fetchWeatherDataApi: Http Timeout", e);
                // Try to read from cache if network fails, even if expired
                if (cacheFile.exists()) {
                    responseText = readFromCacheFile(cacheFile);
                    requestTimestamp = cacheFile.lastModified();
                    Log.i(TAG, "fetchWeatherDataApi: Network timeout, attempting to use expired cache.");
                } else {
                    return null; // No network, no cache
                }
            } catch (Exception e) {
                Log.e(TAG, "fetchWeatherDataApi: Error fetching data", e);
                // Try to read from cache if network fails, even if expired
                if (cacheFile.exists()) {
                    responseText = readFromCacheFile(cacheFile);
                    requestTimestamp = cacheFile.lastModified();
                    Log.i(TAG, "fetchWeatherDataApi: Network error, attempting to use expired cache.");
                } else {
                    return null; // No network, no cache
                }
            }
        }

        // --- Gson Parsing Logic ---
        if (responseText == null || responseText.isEmpty()) {
            Log.w(TAG, "fetchWeatherDataApi: No response text found (empty or null).");
            return null;
        }

        Gson gson = new Gson();
        OpenWeatherMapCurrentResponse apiResponse;
        try {
            apiResponse = gson.fromJson(responseText, OpenWeatherMapCurrentResponse.class);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "fetchWeatherDataApi: Error parsing JSON with Gson", e);
            // Optionally delete corrupt cache file here: cacheFile.delete();
            return null;
        }

        if (apiResponse == null || apiResponse.getMain() == null || apiResponse.getCoord() == null) {
            Log.w(TAG, "fetchWeatherDataApi: API response or essential objects (main, coord) are null after Gson parsing.");
            return null;
        }

        // --- Conversion to WeatherEntry ---
        WeatherEntry entry = new WeatherEntry();

        entry.cityID = apiResponse.getId();
        entry.cityName = apiResponse.getName();

        ApiCoord apiCoord = apiResponse.getCoord();
        if (apiCoord != null) {
            entry.lat = (float) apiCoord.getLat();
            entry.lon = (float) apiCoord.getLon();
        } else {
            entry.lat = lat; // Fallback to input lat/lon
            entry.lon = lon; // Fallback to input lat/lon
        }

        Main apiMain = apiResponse.getMain();
        if (apiMain != null) {
            entry.temperature = apiMain.getTemp();
            entry.apparentTemperature = apiMain.getFeelsLike();
            entry.humidity = apiMain.getHumidity();
        }

        Clouds apiClouds = apiResponse.getClouds();
        if (apiClouds != null) {
            entry.clouds = apiClouds.getAll();
        }

        Wind apiWind = apiResponse.getWind();
        if (apiWind != null) {
            entry.windSpeed = apiWind.getSpeed();
            entry.windDirection = apiWind.getDeg();
        }

        Rain apiRain = apiResponse.getRain();
        if (apiRain != null && apiRain.getVolume1h() != null) {
            entry.rain1h = apiRain.getVolume1h();
        } else {
            entry.rain1h = -1.0;
        }
        // Current weather only has 1h, so 3h remains default or -1.0
        entry.rain3h = -1.0; // The 'Rain' model now supports both, but current weather only has 1h.

        Snow apiSnow = apiResponse.getSnow();
        if (apiSnow != null && apiSnow.getVolume1h() != null) {
            // WeatherEntry does not have a specific snow volume field,
            // you might want to add one or use a generic precipitation field
            // For now, it's logged or ignored if not directly mapped.
            // entry.snow1h = apiSnow.getVolume1h(); // If you add this field to WeatherEntry
        }

        List<Weather> apiWeatherList = apiResponse.getWeather();
        if (apiWeatherList != null && !apiWeatherList.isEmpty()) {
            Weather firstWeather = apiWeatherList.get(0);
            entry.weatherIcon = firstWeather.getIcon();
            entry.description = firstWeather.getDescription();
            entry.weatherIconMeteoconsSymbol = iconToText(firstWeather.getIcon());
        }

        CurrentSys currentSys = apiResponse.getCurrentSys();
        if (currentSys != null) {
            entry.sunriseTime = currentSys.getSunrise();
            entry.sunsetTime = currentSys.getSunset();
        }

        entry.timestamp = apiResponse.getDt();
        entry.request_timestamp = requestTimestamp;

        // Visibility (int) and timezone (long) are available in OpenWeatherMapCurrentResponse
        // but not in WeatherEntry. Extend WeatherEntry if you need them.

        return entry;
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

        String CACHE_FILE_FORECAST = "owm_forecast";
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

    private static double getValue(JSONObject json, String name, double defaultvalue) {
        if (json == null) return defaultvalue;
        try {
            return json.getDouble(name);
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

    /**
     * Finds cities based on a query string using the OpenWeatherMap API and Gson for JSON parsing.
     *
     * @param query The city name or part of it to search for.
     * @return A list of City objects (your application's City model), or an empty list if no cities are found or an error occurs.
     */
    static List<com.firebirdberlin.openweathermapapi.models.City> findCityApi(String query) {
        List<com.firebirdberlin.openweathermapapi.models.City> cities = new ArrayList<>();
        String responseText = "";

        URL url;
        try {
            url = getUrlFindCity(query);
        } catch (MalformedURLException e) {
            Log.e(TAG, "findCityApi: Malformed URL for query " + query, e);
            return cities;
        }

        Log.i(TAG, "findCityApi: requesting " + url.toString());
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);

            int responseCode = urlConnection.getResponseCode();
            String responseMessage = urlConnection.getResponseMessage();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                responseText = getResponseText(urlConnection.getInputStream());
            } else {
                Log.w(TAG, "findCityApi: API responseCode " + responseCode + " - " + responseMessage);
            }
            urlConnection.disconnect();

        } catch (SocketTimeoutException e) {
            Log.e(TAG, "findCityApi: Http Timeout", e);
            return cities;
        } catch (Exception e) {
            Log.e(TAG, "findCityApi: Error fetching data", e);
            return cities;
        }

        // --- Gson Parsing Logic ---
        if (responseText.isEmpty()) {
            Log.w(TAG, "findCityApi: No response text found (empty or null).");
            return cities;
        }

        Gson gson = new Gson();
        OpenWeatherMapFindCityResponse apiResponse;
        try {
            apiResponse = gson.fromJson(responseText, OpenWeatherMapFindCityResponse.class);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "findCityApi: Error parsing JSON with Gson", e);
            return cities;
        }

        if (apiResponse == null || apiResponse.getList() == null) {
            Log.w(TAG, "findCityApi: API response or list object is null after Gson parsing.");
            return cities;
        }

        // --- Conversion to your application's City List ---
        for (FindCityEntry apiCityEntry : apiResponse.getList()) {
            com.firebirdberlin.openweathermapapi.models.City appCity =
                    new com.firebirdberlin.openweathermapapi.models.City(); // Instantiate your app's City

            appCity.id = apiCityEntry.getId();
            appCity.name = apiCityEntry.getName();

            if (apiCityEntry.getSys() != null) {
                appCity.countryCode = apiCityEntry.getSys().getCountry();
            } else {
                appCity.countryCode = ""; // Default if country is missing
            }

            if (apiCityEntry.getCoord() != null) {
                appCity.lat = apiCityEntry.getCoord().getLat();
                appCity.lon = apiCityEntry.getCoord().getLon();
            } else {
                appCity.lat = 0.0f; // Default if coords are missing
                appCity.lon = 0.0f;
            }

            cities.add(appCity);
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
        String APP_ID = BuildConfig.API_KEY_OWM;
        return Uri.parse(ENDPOINT).buildUpon()
                .appendPath(endpoint)
                .appendQueryParameter("appid", APP_ID);
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
