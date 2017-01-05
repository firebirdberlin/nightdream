package com.firebirdberlin.radiostreamapi;

import android.util.Log;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.String;
import java.lang.StringBuilder;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.firebirdberlin.radiostreamapi.models.RadioStation;

public class DirbleApi {

    private static String TAG = "NightDream.DirbleAPI";
    private static String BASEURL = "http://api.dirble.com/v2/search/";
    private static String TOKEN = "770f99fe4971232de187503040";

    public static List<RadioStation> fetchStations(String queryString) {
        List<RadioStation> stationList = new ArrayList<RadioStation>();
        int responseCode = 0;
        String response = "";
        String responseText = "";
        String urlstring = BASEURL + queryString + "?token=" + TOKEN + "&per_page=20";
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
        if (responseCode == 200) {
            Log.i(TAG, " >> responseText " + responseText);
            try {
                JSONArray jsonArray = new JSONArray(responseText);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonStation = jsonArray.getJSONObject(i);
                    JSONArray jsonStreams = jsonStation.getJSONArray("streams");
                    RadioStation station = new RadioStation();
                    station.id = jsonStation.getLong("id");
                    station.name = jsonStation.getString("name");
                    station.countryCode = jsonStation.getString("country");
                    if ( jsonStreams.length() > 0 ) {
                        JSONObject streamObj = jsonStreams.getJSONObject(0);
                        station.stream = streamObj.getString("stream");
                    }
                    stationList.add(station);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, " >> responseCode " + String.valueOf(responseCode));
        }
        return stationList;
    }

    private static String getResponseText(HttpURLConnection c) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();
        return sb.toString();
    }
}
