package com.firebirdberlin.openweathermapapi;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.firebirdberlin.HttpReader;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
    private static final String CACHE_FILE = "BrightSkyApi";
    private static final String TAG = "BrightSkyApi";

    public static WeatherEntry fetchCurrentWeatherData(Context context, float lat, float lon) {
        String responseText = fetchWeatherData(context, lat, lon);
        if (responseText == null) {
            return new WeatherEntry();
        }
        Data data = new Gson().fromJson(responseText, Data.class);
        long now = System.currentTimeMillis();
        Weather weather = data.getLatestWeather(now);
        if (weather != null) {
            Source source = data.getSourceById(weather.source_id);
            return weather.toWeatherEntry(source, lat, lon, now);
        } else {
            WeatherEntry entry = new WeatherEntry();
            entry.lat = lat;
            entry.lon = lon;
            return entry;
        }
    }

    public static List<WeatherEntry> fetchHourlyWeatherData(Context context, float lat, float lon) {
        String responseText = fetchWeatherData(context, lat, lon);
        if (responseText == null) {
            return new ArrayList<>();
        }
        responseText = responseText.replaceAll("\"relative_humidity\": null", "\"relative_humidity\": -1");
        // Log.i(TAG, responseText);
        Gson gson = new GsonBuilder().create();
        Data data = gson.fromJson(responseText, Data.class);
        // data.log();

        long now = System.currentTimeMillis();
        List<WeatherEntry> entries = new ArrayList<>();
        for (Weather weather : data.weather) {
            Source source = data.getSourceById(weather.source_id);
            source.lat = lat;
            source.lon = lon;
            entries.add(weather.toWeatherEntry(source, now));
        }
        return entries;
    }

    private static String fetchWeatherData(Context context, float lat, float lon) {
        String responseText = "";

        Log.d(TAG, "fetchWeatherData(" + lat + "," + lon + ")");
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
        return kilometersPerHour * 1000. / 3600.;
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

    public class Data {
        List<Source> sources;
        List<Weather> weather;

        Source getSourceById(int id) {
            for (Source source : sources) {
                if (source.id == id) {
                    return source;
                }
            }
            return null;
        }

        Weather getLatestWeather(long now) {
            Weather latestWeather = null;
            for (Weather weather : weather) {
                if (weather.timestampToMillis() > now) {
                    break;
                }
                latestWeather = weather;
            }
            return latestWeather;
        }

        // keep it for convenience, Logs all data.
        void log() {
            for (Source source : sources) {
                Log.i(TAG, new Gson().toJson(source));
            }
            for (Weather weather : weather) {
                Log.i(TAG, new Gson().toJson(weather));
            }
        }
    }

    class Source {
        // commented fields are currently not in use
        int id;
        // String dwd_station_id;
        // String wmo_station_id;
        String station_name;
        // String observation_type;
        float lat;
        float lon;
        // float height;
        // int distance;
    }

    class Weather {
        // commented fields are currently not in use
        String timestamp;
        int source_id;
        int cloud_cover;
        String condition;
        // float dew_point;
        String icon;
        float precipitation;
        // float pressure_msl;
        int relative_humidity = -1;
        // int sunshine;
        float temperature;
        // int visibility;
        int wind_direction;
        float wind_speed;
        // int wind_gust_direction;
        // float wind_gust_speed;

        long timestampToMillis() {
            try {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.getDefault());
                Date result = df.parse(timestamp);
                return result.getTime();
            } catch (ParseException e) {
                return -1L;
            }
        }

        WeatherEntry toWeatherEntry(Source source, long now) {
            return toWeatherEntry(source, source.lat, source.lon, now);
        }

        WeatherEntry toWeatherEntry(Source source, float lat, float lon, long now) {
            WeatherEntry entry = new WeatherEntry();
            entry.cityID = (source != null) ? source.id : -1;
            entry.cityName = (source != null) ? source.station_name : String.format(java.util.Locale.getDefault(), "%3.2f, %3.2f", lat, lon);
            entry.clouds = cloud_cover;
            entry.description = condition;
            entry.lat = lat;
            entry.lon = lon;
            entry.rain1h = precipitation;
            entry.rain3h = -1f;
            entry.request_timestamp = now;
            entry.sunriseTime = 0L;
            entry.sunsetTime = 0L;
            entry.temperature = toKelvin(temperature);
            entry.apparentTemperature = -273.15;
            entry.humidity = relative_humidity;
            entry.weatherIcon = icon;
            entry.windDirection = wind_direction;
            entry.windSpeed = toMetersPerSecond(wind_speed);
            entry.timestamp = timestampToMillis() / 1000;
            return entry;
        }
    }
}