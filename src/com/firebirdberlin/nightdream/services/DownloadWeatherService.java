package com.firebirdberlin.nightdream.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.String;
import java.lang.StringBuilder;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

import de.greenrobot.event.EventBus;
import com.firebirdberlin.nightdream.events.OnWeatherDataUpdated;
import com.firebirdberlin.nightdream.models.WeatherEntry;
import com.firebirdberlin.nightdream.Settings;

public class DownloadWeatherService extends Service {
    private static String TAG = "NightDream.DownloadWeatherService";
    private static String BASEURL = "http://api.openweathermap.org/data/2.5/weather?";
    private static String APPID = "645d3eb40425e8af8edc25ddbf153db8";
    private Context mContext = null;

    @Override
    public void onCreate(){

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mContext = this;
        Log.d(TAG, TAG + " started");

        Bundle bundle = intent.getExtras();
        float lat = bundle.getFloat("lat");
        float lon = bundle.getFloat("lon");
        String cityID = bundle.getString("cityID", "");

        if (cityID != "") {
            HttpGetAsyncTask getWeatherTask = new HttpGetAsyncTask(this, cityID);
            getWeatherTask.execute();
        } else {
            HttpGetAsyncTask getWeatherTask = new HttpGetAsyncTask(this, lat, lon);
            getWeatherTask.execute();
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy(){

    }


    class HttpGetAsyncTask extends AsyncTask<String, Void, WeatherEntry>{
        private Context mContext;
        private float lat;
        private float lon;
        private String cityID;

        public HttpGetAsyncTask (Context context, float lat, float lon){
            mContext = context;
            this.lat = lat;
            this.lon = lon;
            this.cityID = "";
        }

        public HttpGetAsyncTask (Context context, String cityID){
            mContext = context;
            this.cityID = cityID;
        }

        @Override
        protected WeatherEntry doInBackground(String... params) {
            WeatherEntry entry = new WeatherEntry();
            int responseCode = 0;
            String response = "";
            String responseText = "";
            String urlstring = BASEURL;
            if (cityID != "") {
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

            }
            if (responseCode != 200) return entry;

            Log.i(TAG, " >> response " + response);
            Log.i(TAG, " >> responseText " + responseText);

            try {
                JSONObject json = new JSONObject(responseText);
                JSONObject jsonMain = json.getJSONObject("main");
                JSONObject jsonWind = json.getJSONObject("wind");
                JSONObject jsonSys = json.getJSONObject("sys");
                JSONArray jsonWeather = json.getJSONArray("weather");

                entry.cityID = json.getInt("id");
                entry.cityName = json.getString("name");
                entry.timestamp = json.getLong("dt");
                entry.temperature = jsonMain.getDouble("temp");
                entry.sunriseTime = jsonSys.getLong("sunrise");
                entry.sunsetTime = jsonSys.getLong("sunset");
                entry.windSpeed = jsonWind.getDouble("speed");
                entry.windDirection = jsonWind.getInt("deg");
                if ( jsonWeather.length() > 0 ) {
                    JSONObject weatherObj = jsonWeather.getJSONObject(0);
                    entry.weatherIcon = weatherObj.getString("icon");
                }

            } catch (Exception e) {
                //Log.e(TAG, "Exception " + e.toString());
                Log.e(TAG, Log.getStackTraceString(e));
                e.printStackTrace();
                // JSONException
            }

            return entry;
        }

        private String getResponseText(HttpURLConnection c) throws IOException {
            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line+"\n");
            }
            br.close();
            return sb.toString();
        }

        @Override
        protected void onPostExecute(WeatherEntry entry) {
            super.onPostExecute(entry);

            if (entry == null){
                stopSelf();
                return;
            }
            if ( entry.timestamp > WeatherEntry.INVALID ) {
                Settings settings = new Settings(mContext);
                settings.setWeatherEntry(entry);
                EventBus.getDefault().post(new OnWeatherDataUpdated(entry));
            }
            Log.d(TAG, "Download finished.");

            stopSelf();
        }
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
}
