package com.firebirdberlin.openweathermapapi;

import android.content.Context;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MetNoApi {
    public static final String ACTION_WEATHER_DATA_UPDATED = "com.firebirdberlin.nightdream.WEATHER_DATA_UPDATED";
    private static final String ENDPOINT = "https://api.met.no/weatherapi/locationforecast/2.0/";
    private static final String CACHE_FILE = "MetNoApi";
    private static final String TAG = "MetNoApi";
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
        if (data != null) {
            return data.getLatestWeather(now, lat, lon);
        }
        return null;
    }

    public static List<WeatherEntry> fetchHourlyWeatherData(Context context, float lat, float lon) {
        String responseText = fetchWeatherData(context, lat, lon);
        if (responseText == null || responseText.isEmpty()) {
            return new ArrayList<>();
        }
        Gson gson = new GsonBuilder().create();
        Data data = gson.fromJson(responseText, Data.class);
        data.log();

        List<WeatherEntry> entries = new ArrayList<>();
        if (data.isValid()) {
            for (Properties.TimeSeries timeSeries : data.properties.timeseries) {
                entries.add(timeSeries.toWeatherEntry(lat, lon, requestTimestamp));
            }
        }
        return entries;
    }

    private static String fetchWeatherData(Context context, float lat, float lon) {
        String responseText;

        Log.d(TAG, "fetchWeatherData(" + lat + "," + lon + ")");
        String cacheFileName = String.format(
                Locale.getDefault(), "%s_%3.2f_%3.2f.txt", CACHE_FILE, lat, lon
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
        Uri.Builder builder = Uri
                .parse(ENDPOINT)
                .buildUpon()
                .appendPath("complete")
                .appendQueryParameter("lat", String.valueOf(lat))
                .appendQueryParameter("lon", String.valueOf(lon));

        String url = builder.build().toString();
        return new URL(url);
    }

    public static class Data {
        Geometry geometry;
        Properties properties;

        boolean isValid() {
            return (
                    properties != null && properties.timeseries != null
                            && !properties.timeseries.isEmpty()
            );
        }

        WeatherEntry getLatestWeather(long now, float lat, float lon) {
            Properties.TimeSeries result = null;
            if (isValid()) {
                for (Properties.TimeSeries timeSeries : properties.timeseries) {
                    if (timeSeries.timestampToMillis() > now) {
                        break;
                    }
                    result = timeSeries;
                }
            }
            if (result != null) {
                return result.toWeatherEntry(lat, lon, requestTimestamp);
            }
            return null;
        }

        // keep it for convenience, Logs all data.
        void log() {
            if (geometry != null)
                Log.i(TAG, new Gson().toJson(geometry));
            if (properties != null)
                Log.i(TAG, new Gson().toJson(properties));
        }
    }

    static class Geometry {
        String type;
        List<Float> coordinates;
    }

    static class Properties {
        Meta meta;
        List<TimeSeries> timeseries;

        static class Meta {
            String updated_at;
            Units units;

            static class Units {
                String air_pressure_at_sea_level;
                String air_temperature;
                String cloud_area_fraction;
                String precipitation_amount;
                String relative_humidity;
                String wind_from_direction;
                String wind_speed;
            }
        }

        static class TimeSeries {
            String time;
            Data data;

            long timestampToMillis() {
                try {
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
                    Date result = df.parse(time.replace("Z", "+00:00"));
                    Log.w(TAG, time + " => " + (result != null ? result.getTime() : 0));
                    return result != null ? result.getTime() : -1L;
                } catch (ParseException e) {
                    return -1L;
                }
            }

            WeatherEntry toWeatherEntry(float lat, float lon, long requestTimestamp) {
                WeatherEntry entry = new WeatherEntry();
                entry.cityID = -1;
                entry.cityName = null;
                entry.lat = lat;
                entry.lon = lon;

                if (data.next_1_hours != null && data.next_1_hours.details != null) {
                    entry.rain1h = data.next_1_hours.details.precipitation_amount;
                } else if (data.next_6_hours != null && data.next_6_hours.details != null) {
                    entry.rain1h = data.next_6_hours.details.precipitation_amount;
                } else {
                    entry.rain1h = -1f;
                }

                entry.rain3h = -1f;
                entry.request_timestamp = requestTimestamp;
                entry.sunriseTime = 0L;
                entry.sunsetTime = 0L;
                entry.apparentTemperature = -273.15;

                if (mContext != null && mContext.get() != null) {
                    City city = GeocoderApi.findCityByCoordinates(mContext.get(), lat, lon);
                    if (city != null && !Utility.isEmpty(city.name)) entry.cityName = city.name;
                }

                entry.clouds = Math.round(data.instant.details.cloud_area_fraction);
                if (data.next_1_hours != null && data.next_1_hours.summary != null && data.next_1_hours.summary.symbol_code != null) {
                    entry.weatherIcon = data.next_1_hours.summary.symbol_code;
                    entry.weatherIconMeteoconsSymbol = symbolCodeToMeteoconsChar(data.next_1_hours.summary.symbol_code);
                } else
                if (data.next_6_hours != null && data.next_6_hours.summary != null && data.next_6_hours.summary.symbol_code != null) {
                    entry.weatherIcon = data.next_6_hours.summary.symbol_code;
                    entry.weatherIconMeteoconsSymbol = symbolCodeToMeteoconsChar(data.next_6_hours.summary.symbol_code);
                } else {
                    entry.weatherIcon = "";
                }

                entry.description = null;
                entry.humidity = Math.round(data.instant.details.relative_humidity);
                entry.temperature = toKelvin(data.instant.details.air_temperature);
                entry.timestamp = timestampToMillis() / 1000;
                entry.windDirection = Math.round(data.instant.details.wind_from_direction);
                entry.windSpeed = data.instant.details.wind_speed;
                return entry;
            }

            public String symbolCodeToMeteoconsChar(String code) {
                // map polar twilight to day
                code = code.replace("_polartwilight", "");
                if (Utility.equalsAny(code, "clearsky", "clearsky_day")) return "B";
                if (Utility.equalsAny(code, "fair", "fair_day")) return "H";
                if (Utility.equalsAny(code, "heavyrain", "heavyrain_day", "heavyrain_night")) return "R";
                if (Utility.equalsAny(code, "heavyrainandthunder", "heavyrainandthunder_day", "heavyrainandthunder_night")) return "T";
                if (Utility.equalsAny(code, "heavyrainshowers", "heavyrainshowers_day", "heavyrainshowers_night")) return "R";
                if (Utility.equalsAny(code, "heavyrainshowersandthunder", "heavyrainshowersandthunder_day", "heavyrainshowersandthunder_night")) return "T";
                if (Utility.equalsAny(code, "heavysleet", "heavysleet_day", "heavysleet_night")) return "U";
                if (Utility.equalsAny(code, "heavysleetandthunder", "heavysleetandthunder_day", "heavysleetandthunder_night")) return "0";
                if (Utility.equalsAny(code, "heavysleetshowers", "heavysleetshowers_day", "heavysleetshowers_night")) return "W";
                if (Utility.equalsAny(code, "heavysleetshowersandthunder", "heavysleetshowersandthunder_day", "heavysleetshowersandthunder_night")) return "0";
                if (Utility.equalsAny(code, "heavysnow", "heavysnow_day", "heavysnow_night")) return "W";
                if (Utility.equalsAny(code, "heavysnowandthunder", "heavysnowandthunder_day", "heavysnowandthunder_night")) return "0";
                if (Utility.equalsAny(code, "heavysnowshowers", "heavysnowshowers_day", "heavysnowshowers_night")) return "W";
                if (Utility.equalsAny(code, "heavysnowshowersandthunder", "heavysnowshowersandthunder_day", "heavysnowshowersandthunder_night")) return "0";
                if (Utility.equalsAny(code, "lightrain", "lightrain_day", "lightrain_night")) return "Q";
                if (Utility.equalsAny(code, "lightrainandthunder", "lightrainandthunder_day", "lightrainandthunder_night")) return "O";
                if (Utility.equalsAny(code, "lightrainshowers", "lightrainshowers_day", "lightrainshowers_night")) return "Q";
                if (Utility.equalsAny(code, "lightrainshowersandthunder", "lightrainshowersandthunder_day", "lightrainshowersandthunder_night")) return "O";
                if (Utility.equalsAny(code, "lightsleet", "lightsleet_day", "lightsleet_night")) return "V";
                if (Utility.equalsAny(code, "lightsleetandthunder", "lightsleetandthunder_day", "lightsleetandthunder_night")) return "O";
                if (Utility.equalsAny(code, "lightsleetshowers", "lightsleetshowers_day", "lightsleetshowers_night")) return "V";
                if (Utility.equalsAny(code, "lightsnow", "lightsnow_day", "lightsnow_night")) return "V";
                if (Utility.equalsAny(code, "lightsnowandthunder", "lightsnowandthunder_day", "lightsnowandthunder_night")) return "O";
                if (Utility.equalsAny(code, "lightsnowshowers", "lightsnowshowers_day", "lightsnowshowers_night")) return "V";
                if (Utility.equalsAny(code, "lightssleetshowersandthunder", "lightssleetshowersandthunder_day", "lightssleetshowersandthunder_night")) return "O";
                if (Utility.equalsAny(code, "lightssnowshowersandthunder", "lightssnowshowersandthunder_day", "lightssnowshowersandthunder_night")) return "O";
                if (Utility.equalsAny(code, "partlycloudy", "partlycloudy_day")) return "H";
                if (Utility.equalsAny(code, "rainandthunder", "rainandthunder_day", "rainandthunder_night")) return "O";
                if (Utility.equalsAny(code, "rainshowers", "rainshowers_day", "rainshowers_night")) return "R";
                if (Utility.equalsAny(code, "rainshowersandthunder", "rainshowersandthunder_day", "rainshowersandthunder_night")) return "O";
                if (Utility.equalsAny(code, "sleetandthunder", "sleetandthunder_day", "sleetandthunder_night")) return "O";
                if (Utility.equalsAny(code, "sleetshowers", "sleetshowers_day", "sleetshowers_night")) return "X";
                if (Utility.equalsAny(code, "sleetshowersandthunder", "sleetshowersandthunder_day", "sleetshowersandthunder_night")) return "O";
                if (Utility.equalsAny(code, "snowandthunder", "snowandthunder_day", "snowandthunder_night")) return "O";
                if (Utility.equalsAny(code, "snowshowers", "snowshowers_day")) return "U";
                if (Utility.equalsAny(code, "snowshowersandthunder", "snowshowersandthunder_day", "snowshowersandthunder_night")) return "O";

                switch (code) {
                    case "clearsky_night":
                        return "C";
                    case "cloudy":
                        return "N";
                    case "fair_night":
                        return "I";
                    case "fog":
                        return "M";
                    case "partlycloudy_night":
                        return "I";
                    case "rain":
                        return "R";
                    case "sleet":
                        return "X";
                    case "snow":
                        return "W";
                }
                return "";
            }

            static class Data {
                Instant instant;
                Next_1_hours next_1_hours;
                Next_6_hours next_6_hours;

                static class Instant {
                    Details details;

                    static class Details {
                        float air_pressure_at_sea_level;
                        float air_temperature;
                        float cloud_area_fraction;
                        float relative_humidity;
                        float wind_from_direction;
                        float wind_speed;
                    }
                }

                static class Next_1_hours {
                    Summary summary;
                    Details details;

                    static class Summary {
                        String symbol_code;

                    }

                    static class Details {
                        float precipitation_amount;
                        float precipitation_amount_max;
                        float precipitation_amount_min;
                        float probability_of_precipitation;
                        float probability_of_thunder;
                    }
                }
                static class Next_6_hours {
                    Summary summary;
                    Details details;

                    static class Summary {
                        String symbol_code;
                    }

                    static class Details {
                        float precipitation_amount;
                        float precipitation_amount_max;
                        float precipitation_amount_min;
                        float probability_of_precipitation;
                        float probability_of_thunder;
                    }
                }
            }
        }
    }
}