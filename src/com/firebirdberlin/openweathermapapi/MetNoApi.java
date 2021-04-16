package com.firebirdberlin.openweathermapapi;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import com.firebirdberlin.HttpReader;
import com.firebirdberlin.nightdream.Utility;
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

public class MetNoApi {
    public static final String ACTION_WEATHER_DATA_UPDATED = "com.firebirdberlin.nightdream.WEATHER_DATA_UPDATED";
    private static final String ENDPOINT = "https://api.met.no/weatherapi/locationforecast/2.0/";
    private static final String CACHE_FILE = "MetNoApi";
    private static final String TAG = "MetNoApi";
    private static WeakReference<Context> mContext;

    public static WeatherEntry fetchCurrentWeatherData(Context context, float lat, float lon) {
        mContext = new WeakReference<>(context);
        String responseText = fetchWeatherData(context, lat, lon);
        if (responseText == null || responseText.isEmpty()) {
            return new WeatherEntry();
        }
        Data data = new Gson().fromJson(responseText, Data.class);
        long now = System.currentTimeMillis();
        Weather weather = null;
        if (data != null) {
            weather = data.getLatestWeather(now);
        }
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
        if (responseText == null || responseText.isEmpty()) {
            return new ArrayList<>();
        }
        responseText = responseText.replaceAll("\"relative_humidity\": null", "\"relative_humidity\": -1");
        // Log.i(TAG, responseText);
        Gson gson = new GsonBuilder().create();
        Data data = gson.fromJson(responseText, Data.class);
        data.log();

        data.weather = data.timeseriesToWeather();

        long now = System.currentTimeMillis();
        List<WeatherEntry> entries = new ArrayList<>();
        if (data != null && data.weather != null) {
            for (Weather weather : data.weather) {
                Source source = data.getSourceById(weather.source_id);
                source.lat = lat;
                source.lon = lon;
                entries.add(weather.toWeatherEntry(source, now));
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
        Uri.Builder builder = Uri
                .parse(ENDPOINT)
                .buildUpon()
                .appendPath("compact")
                .appendQueryParameter("lat", String.valueOf(lat))
                .appendQueryParameter("lon", String.valueOf(lon));

        String url = builder.build().toString();
        return new URL(url);
    }

    public class Data {
        List<Source> sources;
        List<Weather> weather;
        Geometry geometry;
        Properties properties;

        boolean isValid() {
            return (
                    weather != null
                            && sources != null
                            && weather.size() > 0
                            && sources.size() > 0
            );
        }

        Source getSourceById(int id) {
            if (geometry != null) {
                Source source = new Source();
                source.lon = geometry.coordinates.get(0);
                source.lat = geometry.coordinates.get(1);
                return source;
            }
            return null;
        }

        Weather getLatestWeather(long now) {
            Weather latestWeather = new Weather();
            if (properties != null) {
                for (Properties.Timeseries weather : properties.timeseries) {
                    if (weather.timestampToMillis() > now) {
                        break;
                    }
                    latestWeather.timestamp = weather.time;
                    latestWeather.cloud_cover = Math.round(weather.data.instant.details.cloud_area_fraction);
                    if (weather.data.next_1_hours != null && weather.data.next_1_hours.summary != null && weather.data.next_1_hours.summary.symbol_code != null) {
                        latestWeather.icon = weather.data.next_1_hours.summary.symbol_code;
                        latestWeather.condition = weather.data.next_1_hours.summary.symbol_code;
                    } else {
                        latestWeather.icon = "";
                    }
                    latestWeather.relative_humidity = Math.round(weather.data.instant.details.relative_humidity);
                    latestWeather.temperature = weather.data.instant.details.air_temperature;
                    latestWeather.wind_direction = Math.round(weather.data.instant.details.wind_from_direction);
                    latestWeather.wind_speed = weather.data.instant.details.wind_speed;
                }
            }
            return latestWeather;
        }

        List<Weather> timeseriesToWeather() {
            this.weather = new ArrayList<>();
            if (properties != null) {
                for (Properties.Timeseries weather : properties.timeseries) {
                    if (weather.timestampToMillis() > 2 * 24 * 60 * 60 * 1000 + System.currentTimeMillis()) {
                        break;
                    }
                    Weather latestWeather = new Weather();
                    latestWeather.timestamp = weather.time;
                    latestWeather.cloud_cover = Math.round(weather.data.instant.details.cloud_area_fraction);
                    if (weather.data.next_1_hours != null && weather.data.next_1_hours.summary != null && weather.data.next_1_hours.summary.symbol_code != null) {
                        latestWeather.icon = weather.data.next_1_hours.summary.symbol_code;
                        latestWeather.condition = weather.data.next_1_hours.summary.symbol_code;
                    } else {
                        latestWeather.icon = "";
                    }
                    latestWeather.relative_humidity = Math.round(weather.data.instant.details.relative_humidity);
                    latestWeather.temperature = weather.data.instant.details.air_temperature;
                    latestWeather.wind_direction = Math.round(weather.data.instant.details.wind_from_direction);
                    latestWeather.wind_speed = weather.data.instant.details.wind_speed;
                    this.weather.add(latestWeather);
                }
            }
            return this.weather;
        }

        // keep it for convenience, Logs all data.
        void log() {
            if (sources != null)
                for (Source source : sources) {
                    Log.i(TAG, new Gson().toJson(source));
                }
            if (weather != null)
                for (Weather weather : weather) {
                    Log.i(TAG, new Gson().toJson(weather));
                }
            if (geometry != null)
                Log.i(TAG, new Gson().toJson(geometry));
            if (properties != null)
                Log.i(TAG, new Gson().toJson(properties));
        }
    }

    class Geometry {
        String type;
        List<Float> coordinates;
    }

    class Properties {
        Meta meta;
        List<Timeseries> timeseries;

        class Meta {
            String updated_at;
            Units units;

            class Units {
                String air_pressure_at_sea_level;
                String air_temperature;
                String cloud_area_fraction;
                String precipitation_amount;
                String relative_humidity;
                String wind_from_direction;
                String wind_speed;
            }
        }

        class Timeseries {
            String time;
            Data data;

            long timestampToMillis() {
                try {
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault());
                    Date result = df.parse(time);
                    return result.getTime();
                } catch (ParseException e) {
                    Log.e(TAG, e.toString());
                    return -1L;
                }
            }

            class Data {
                Instant instant;
                Next_1_hours next_1_hours;

                class Instant {
                    Details details;

                    class Details {
                        float air_pressure_at_sea_level;
                        float air_temperature;
                        float cloud_area_fraction;
                        float relative_humidity;
                        float wind_from_direction;
                        float wind_speed;
                    }
                }

                class Next_1_hours {
                    Summary summary;

                    class Summary {
                        String symbol_code;
                    }
                }
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
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault());
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

            if (mContext != null && mContext.get() != null) {
                Utility.GeoCoder geoCoder = new Utility.GeoCoder(mContext.get(), lat, lon);
                if (!geoCoder.getLocality().isEmpty()) entry.cityName = geoCoder.getLocality();
            }

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