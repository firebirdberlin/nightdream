package com.firebirdberlin.openweathermapapi;


import android.net.Uri;
import android.util.Log;

import com.firebirdberlin.openweathermapapi.models.City;

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
    private static final String ENDPOINT = "http://api.openweathermap.org/data/2.5/find";
    private static String TAG = "OpenWeatherMapApi";
    private static String APPID = "645d3eb40425e8af8edc25ddbf153db8";
    private static int READ_TIMEOUT = 10000;
    private static int CONNECT_TIMEOUT = 10000;


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
            if ( responseCode == 200 ) {
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
        if (responseCode == 200) {
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
        String url = Uri.parse(ENDPOINT).buildUpon()
                        .appendQueryParameter("q", query)
                        .appendQueryParameter("type", "like")
                        .appendQueryParameter("cnt", "15")
                        .appendQueryParameter("sort", "population")
                        .appendQueryParameter("APPID", APPID)
                        .build().toString();
        return new URL(url);
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
