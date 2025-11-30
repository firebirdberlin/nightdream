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

package com.firebirdberlin.openweathermapapi;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import com.firebirdberlin.HttpReader;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.ref.WeakReference;
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
    private static WeakReference<Context> mContext;
    private static long requestTimestamp = 0L;

    public static WeatherEntry fetchCurrentWeatherData(Context context, float lat, float lon) {
        mContext = new WeakReference<>(context);
        String responseText = fetchWeatherData(context, lat, lon);
        if (responseText == null || responseText.isEmpty()) {
            return new WeatherEntry();
        }
        Data data = new Gson().fromJson(responseText, Data.class);
        long now = System.currentTimeMillis();
        Weather weather = data.getLatestWeather(now);
        if (weather != null) {
            Source source = data.getSourceById(weather.source_id);
            return weather.toWeatherEntry(source, lat, lon, requestTimestamp);
        } else {
            WeatherEntry entry = new WeatherEntry();
            entry.lat = lat;
            entry.lon = lon;
            return entry;
        }
    }

    public static List<WeatherEntry> fetchHourlyWeatherData(Context context, float lat, float lon) {
        String responseText = fetchWeatherData(context, lat, lon);
        if (responseText == null || responseText.isEmpty()) {
            return new ArrayList<>();
        }
        responseText = responseText.replaceAll("\"relative_humidity\": null", "\"relative_humidity\": -1");
        // Log.i(TAG, responseText);
        Gson gson = new GsonBuilder().create();
        Data data = gson.fromJson(responseText, Data.class);
        // data.log();

        List<WeatherEntry> entries = new ArrayList<>();
        if (data != null && data.isValid()) {
            for (Weather weather : data.weather) {
                Source source = data.getSourceById(weather.source_id);
                source.lat = lat;
                source.lon = lon;
                entries.add(weather.toWeatherEntry(source, requestTimestamp));
            }
        }
        return entries;
    }

    private static String fetchWeatherData(Context context, float lat, float lon) {
        String responseText;

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
            requestTimestamp = httpReader.getRequestTimestamp();
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

        boolean isValid() {
            return (
                    weather != null
                            && sources != null
                            && weather.size() > 0
                            && sources.size() > 0
            );
        }

        Source getSourceById(int id) {
            if (sources == null) {
                return null;
            }
            for (Source source : sources) {
                if (source.id == id) {
                    return source;
                }
            }
            return null;
        }

        Weather getLatestWeather(long now) {
            Weather latestWeather = null;
            if (weather != null) {
                for (Weather weather : weather) {
                    if (weather.timestampToMillis() > now) {
                        break;
                    }
                    latestWeather = weather;
                }
            }
            return latestWeather;
        }

        // keep it for convenience, Logs all data.
        void log() {
            if (!isValid()) return;
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
                Log.w(TAG, timestamp + " => " + result.getTime());
                return result.getTime();
            } catch (ParseException e) {
                return -1L;
            }
        }

        WeatherEntry toWeatherEntry(Source source, long requestTimestamp) {
            return toWeatherEntry(source, source.lat, source.lon, requestTimestamp);
        }

        WeatherEntry toWeatherEntry(Source source, float lat, float lon, long requestTimestamp) {
            WeatherEntry entry = new WeatherEntry();
            entry.cityID = (source != null) ? source.id : -1;
            entry.cityName = (source != null) ? source.station_name : String.format(java.util.Locale.getDefault(), "%3.2f, %3.2f", lat, lon);

            if (mContext != null && mContext.get() != null) {
                City city = GeocoderApi.findCityByCoordinates(mContext.get(), lat, lon);
                if (city != null && !Utility.isEmpty(city.name)) entry.cityName = city.name;
            }

            entry.clouds = cloud_cover;
            entry.description = condition;
            if (mContext != null && mContext.get() != null) {
                Resources res = mContext.get().getResources();
                String text = "brightsky_conditions_" + entry.description;
                int resID = res.getIdentifier(text, "string", mContext.get().getPackageName());
                if (resID != 0) entry.description = mContext.get().getResources().getString(resID);
            }
            entry.lat = lat;
            entry.lon = lon;
            entry.rain1h = precipitation;
            entry.rain3h = -1f;
            entry.request_timestamp = requestTimestamp;
            entry.sunriseTime = 0L;
            entry.sunsetTime = 0L;
            entry.temperature = toKelvin(temperature);
            entry.apparentTemperature = -273.15;
            entry.humidity = relative_humidity;
            entry.weatherIcon = icon;
            entry.weatherIconMeteoconsSymbol = iconToMeteoconsSymbol(icon);
            entry.windDirection = wind_direction;
            entry.windSpeed = toMetersPerSecond(wind_speed);
            entry.timestamp = timestampToMillis() / 1000;
            return entry;
        }

        String iconToMeteoconsSymbol(String code) {
            if (code.equals("clear-day")) return "B";
            if (code.equals("clear-night")) return "C";
            if (code.equals("rain")) return "R";
            if (code.equals("snow")) return "W";
            if (code.equals("sleet")) return "X";
            if (code.equals("wind")) return "F";
            if (code.equals("fog")) return "M";
            if (code.equals("cloudy")) return "N";
            if (code.equals("partly-cloudy-day")) return "H";
            if (code.equals("partly-cloudy-night")) return "I";
            if (code.equals("thunderstorm")) return "0";
            if (code.equals("tornado")) return "0";
            if (code.equals("hail")) return "X";
            return "";
        }
    }
}