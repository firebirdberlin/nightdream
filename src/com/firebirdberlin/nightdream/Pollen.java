package com.firebirdberlin.nightdream;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Pollen {

    private static String TAG = "PollenObject";

    private ArrayList<HashMap<String, String>> pollenList;
    private String nextUpdate;

    public Pollen () {
    }

    public boolean setupPollen (String result) {

        Log.d(TAG, "result: "+result);

        ArrayList<HashMap<String, String>> pollenList;
        pollenList = new ArrayList<>();

        if (result != null) {
            try {
                JSONObject jsonObj = new JSONObject(result);
                this.nextUpdate = jsonObj.getString("next_update");

                JSONArray content = jsonObj.getJSONArray("content");

                for (int i = 0; i < content.length(); i++) {
                    JSONObject c = content.getJSONObject(i);
                    String partregion_name = c.getString("partregion_name");

                    if (partregion_name.equals("Rhein.-Westfäl. Tiefland")) {
                        JSONObject pollen = c.getJSONObject("Pollen");

                        HashMap<String, String> pollenTmp = new HashMap<>();

                        Iterator<String> iter = pollen.keys();
                        while (iter.hasNext()) {
                            String key = iter.next();
                            try {
                                String herb = null;

                                switch (key){
                                    case "Ambrosia": herb = "ambrosia"; break;
                                    case "Beifuss": herb = "mugwort"; break;
                                    case "Birke": herb = "birch"; break;
                                    case "Erle": herb = "alder"; break;
                                    case "Esche": herb = "ash"; break;
                                    case "Graeser": herb = "grass"; break;
                                    case "Hasel": herb = "hazelnut"; break;
                                    case "Roggen": herb = "rye"; break;
                                }

                                JSONObject forecast = pollen.getJSONObject(key);
                                pollenTmp.put(herb, forecast.getString("today"));
                            } catch (JSONException e) {
                                Log.e(TAG, "Json pollen error: " + e.getMessage());
                            }
                        }
                        pollenList.add(pollenTmp);
                    }
                    this.pollenList = new ArrayList<>();
                    this.pollenList.addAll(pollenList);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Json parsing error: " + e.getMessage());
                return false;
            }
        } else {
            Log.e(TAG, "Couldn't get json from server.");
            return false;
        }
        return true;
    }

    public ArrayList<HashMap<String, String>> getPollenList (){
        return this.pollenList;
    };

    public String getNextUpdate () {
        return nextUpdate;
    }

}
