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
    private ArrayList<HashMap<String, String>> pollenAreaList;
    private String nextUpdate;
    private String postCode = "";

    public Pollen () {
    }

    public boolean setupPollen (String result, String plz) {

        Log.d(TAG, "result: "+result);

        ArrayList<HashMap<String, String>> pollenList;
        ArrayList<HashMap<String, String>> pollenAreaList;
        pollenList = new ArrayList<>();
        pollenAreaList = new ArrayList<>();

        Integer area = plzToArea(Integer.parseInt(plz.substring(0,2)));
        Log.d(TAG, "pollen area: "+area);

        if (result != null && area != -1) {
            try {
                JSONObject jsonObj = new JSONObject(result);
                this.nextUpdate = jsonObj.getString("next_update");

                JSONArray content = jsonObj.getJSONArray("content");

                for (int i = 0; i < content.length(); i++) {
                    JSONObject c = content.getJSONObject(i);
                    String region_name = c.getString("partregion_name");
                    int region_ID = c.getInt("partregion_id");

                    if (region_ID == -1){
                        region_ID = c.getInt("region_id");
                        region_name = c.getString("region_name");
                    }

                    HashMap<String, String> pollenAreaTmp = new HashMap<>();
                    pollenAreaTmp.put(region_name , Integer.toString(region_ID));
                    pollenAreaList.add(pollenAreaTmp);

                    if (region_ID == area) {
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

                    this.pollenAreaList = new ArrayList<>();
                    this.pollenAreaList.addAll(pollenAreaList);

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

    private static boolean isBetween(int x, int lower, int upper) {
        return lower <= x && x <= upper;
    }

    private Integer plzToArea (Integer area)
    {
        Log.d(TAG, "pollen plzto area: "+area);

        //Inseln und Marschen
        if (isBetween(area, 25, 25)){
            return 11;
        }
        //Geest, Schleswig-Holstein und Hamburg plz 20 -24
        if (isBetween(area, 20, 24)){
            return 12;
        }
        //Mecklenburg-Vorpommern
        if (isBetween(area, 17, 19)){
            return 20;
        }
        //Rhein.-Westfäl. Tiefland
        if (isBetween(area, 40, 53)){
            return 41;
        }
        //Brandenburg und Berlin
        if (isBetween(area, 10, 16) || area == 39){
            return 50;
        }

        return -1;
    }

    public ArrayList<HashMap<String, String>> getPollenList (){
        return this.pollenList;
    };

    public ArrayList<HashMap<String, String>> getPollenAreaList (){
        return this.pollenAreaList;
    };

    public String getNextUpdate () {
        return nextUpdate;
    }

    public String getPostCode () {
        return postCode;
    }

    public void setPostCode (String postCode) {
         this.postCode = postCode;
    }

}
