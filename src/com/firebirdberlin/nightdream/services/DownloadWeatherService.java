package com.firebirdberlin.nightdream.services;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.models.WeatherEntry;
import com.firebirdberlin.openweathermapapi.OpenWeatherMapApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class DownloadWeatherService extends IntentService {
    private static String TAG = "DownloadWeatherService";
    private static String BASEURL = "http://api.openweathermap.org/data/2.5/weather?";
    private static String APPID = "645d3eb40425e8af8edc25ddbf153db8";
    private Context mContext = null;

    private float lat;
    private float lon;
    private String cityID;

    public DownloadWeatherService() {
        super("DownloadWeatherService");
    }

    public DownloadWeatherService(String name) {
        super(name);
    }

    public static void start(Context context, Location location) {
        Intent i = new Intent(context, DownloadWeatherService.class);
        i.putExtra("lat", (float) location.getLatitude());
        i.putExtra("lon", (float) location.getLongitude());
        context.startService(i);
    }

    public static void start(Context context, String cityID) {
        Intent i = new Intent(context, DownloadWeatherService.class);
        i.putExtra("cityID", cityID);
        context.startService(i);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mContext = this;
        Log.d(TAG, TAG + " started");

        Bundle bundle = intent.getExtras();
        lat = bundle.getFloat("lat");
        lon = bundle.getFloat("lon");
        cityID = bundle.getString("cityID", "");
        WeatherEntry entry = fetch();
        onPostExecute(entry);
    }

    protected WeatherEntry fetch() {
        WeatherEntry entry = new WeatherEntry();
        int responseCode = 0;
        String response = "";
        String responseText = "";
        String urlstring = BASEURL;
        if (!cityID.isEmpty()) {
            urlstring += "id=" + cityID + "&";
        } else {
            urlstring += "lat=" + String.valueOf(lat) + "&" +
                "lon=" + String.valueOf(lon) + "&";
        }
        urlstring += "appid=" + APPID;

        Log.i(TAG, "requesting " + urlstring);
        try {
            URL url = new URL(urlstring);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            response = urlConnection.getResponseMessage();
            responseCode = urlConnection.getResponseCode();
            if ( responseCode == 200 ) {
                responseText = getResponseText(urlConnection);
            }
            urlConnection.disconnect();
        }
        catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            e.printStackTrace();
        }

        Log.i(TAG, " >> response " + response);
        if (responseCode != 200) {
            Log.w(TAG, " >> responseCode " + String.valueOf(responseCode));
            return entry;
        } else {
            Log.i(TAG, " >> responseText " + responseText);
        }


        JSONObject json = getJSONObject(responseText);
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
        entry.windDirection = getValue(jsonWind, "deg", 0);

        entry.weatherIcon = "";
        if ( jsonWeather != null && jsonWeather.length() > 0 ) {
            JSONObject weatherObj = getJSONObject(jsonWeather, 0);
            entry.weatherIcon = getValue(weatherObj, "icon", "");
        }

        return entry;
    }

    private JSONObject getJSONObject(String string_representation) {
        try {
            return new JSONObject(string_representation);
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONObject getJSONObject(JSONArray jsonArray, int index) {
        try {
            return jsonArray.getJSONObject(index);
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONObject getJSONObject(JSONObject json, String name) {
        try {
            return json.getJSONObject(name);
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONArray getJSONArray(JSONObject json, String name) {
        try {
            return json.getJSONArray(name);
        } catch (JSONException e) {
            return null;
        }
    }

    private double getValue(JSONObject json, String name, double defaultvalue) {
        if ( json == null ) return defaultvalue;
        try {
            return json.getDouble(name);
        } catch (JSONException e) {
            return defaultvalue;
        }
    }

    private int getValue(JSONObject json, String name, int defaultvalue) {
        if ( json == null ) return defaultvalue;
        try {
            return json.getInt(name);
        } catch (JSONException e) {
            return defaultvalue;
        }
    }

    private String getValue(JSONObject json, String name, String defaultvalue) {
        if ( json == null ) return defaultvalue;
        try {
            return json.getString(name);
        } catch (JSONException e) {
            return defaultvalue;
        }
    }

    private long getValue(JSONObject json, String name, long defaultvalue) {
        if ( json == null ) return defaultvalue;
        try {
            return json.getLong(name);
        } catch (JSONException e) {
            return defaultvalue;
        }
    }

    private String getResponseText(HttpURLConnection c) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    protected void onPostExecute(WeatherEntry entry) {

        if (entry == null){
            return;
        }
        if ( entry.timestamp > WeatherEntry.INVALID ) {
            Settings settings = new Settings(mContext);
            settings.setWeatherEntry(entry);
            broadcastResult();
        } else {
            Log.w(TAG, "entry.timestamp is INVALID!");

        }

        Log.d(TAG, "Download finished.");

    }

    private void broadcastResult() {
        Intent i = new  Intent(OpenWeatherMapApi.ACTION_WEATHER_DATA_UPDATED);
        sendBroadcast(i);
    }
}
