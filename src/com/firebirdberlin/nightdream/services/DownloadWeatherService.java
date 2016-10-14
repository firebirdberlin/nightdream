package com.firebirdberlin.nightdream.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
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


public class DownloadWeatherService extends Service {
    private static String TAG = "NightDream.DownloadWeatherService";
    private Context mContext = null;

    @Override
    public void onCreate(){

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mContext = this;
        Log.d(TAG, TAG + " started");

        HttpGetAsyncTask getWeatherTask = new HttpGetAsyncTask(this);
        getWeatherTask.execute();

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy(){

    }


    class HttpGetAsyncTask extends AsyncTask<String, Void, String>{
        private Context mContext;

        public HttpGetAsyncTask (Context context){
            mContext = context;
        }

        @Override
        protected String doInBackground(String... params) {
            String response = "";
            String responseText = "";
            String urlstring = "http://api.openweathermap.org/data/2.5/weather?" +
                               "lat=52.5&" +
                               "lon=13.4&" +
                               "appid=645d3eb40425e8af8edc25ddbf153db8";
            Log.i(TAG, "requesting " + urlstring);
            try {
                URL url = new URL(urlstring);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                //InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                //readStream(in);
                response = urlConnection.getResponseMessage();
                if ( urlConnection.getResponseCode() == 200 ) {
                    responseText = getResponseText(urlConnection);
                }
                urlConnection.disconnect();
            }
            catch (Exception e) {

            }
            Log.i(TAG, " >> response " + response);
            Log.i(TAG, " >> responseText " + responseText);

            try {
                JSONObject json = new JSONObject(responseText);

                JSONObject jsonMain = json.getJSONObject("main");
                String temp = jsonMain.getString("temp");
                JSONObject jsonSys = json.getJSONObject("sys");
                String sunrise = jsonSys.getString("sunrise");
                String sunset = jsonSys.getString("sunset");
                Log.i(TAG, " >> sunrise = " + sunrise );
                Log.i(TAG, " >> sunset = " + sunset );
                Log.i(TAG, " >> temperature = " +  temp +" K");
                JSONArray jsonWeather = json.getJSONArray("weather");
                for (int i = 0; i < jsonWeather.length() ; i++ ) {
                    JSONObject weatherObj = jsonWeather.getJSONObject(i);
                    Log.i(TAG, " >> main:" + weatherObj.getString("main"));
                    Log.i(TAG, " >> description:" + weatherObj.getString("description"));
                    Log.i(TAG, " >> icon:" + weatherObj.getString("icon"));
                }

            } catch (Exception e) {
                //Log.e(TAG, "Exception " + e.toString());
                Log.e(TAG, Log.getStackTraceString(e));
                e.printStackTrace();
                // JSONException
            }

            return response;
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
        protected void onPostExecute(String response) {
            super.onPostExecute(response);

            if (response == null){
                stopSelf();
                return;
            }

            Log.d(TAG, "Download finished.");

            stopSelf();
        }
    }

    public static void start(Context context) {
        Intent i = new Intent(context, DownloadWeatherService.class);
        //i.putExtra("start alarm", true);
        context.startService(i);
    }
}
