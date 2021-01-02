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

        Log.d(TAG, "Pollensetup - "+result);

        ArrayList<HashMap<String, String>> pollenList;
        ArrayList<HashMap<String, String>> pollenAreaList;
        pollenList = new ArrayList<>();
        pollenAreaList = new ArrayList<>();

        Integer area = plzToArea(Integer.parseInt(plz.substring(0,2)));
        Log.d(TAG, "Area: "+area);

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
        //Westl. Niedersachsen und Bremen
        if (isBetween(area, 26, 29)){
            return 31;
        }
        //Östl. Niedersachsen
        if (isBetween(area, 30, 31) || isBetween(area, 37, 38) ){
            return 32;
        }
        //Rhein.-Westfäl. Tiefland
        if (isBetween(area, 40, 52)){
            return 41;
        }
        //Ostwestfalen
        if (isBetween(area, 32, 33) || isBetween(area, 57, 59) ){
            return 42;
        }
        //Mittelgebirge NRW
        if (isBetween(area, 53, 53)){
            return 43;
        }
        //Brandenburg und Berlin
        if (isBetween(area, 10, 16)){
            return 50;
        }
        //Tiefland Sachsen-Anhalt
        if (area == 39){
            return 61;
        }
        //Harz
        if (isBetween(area, 6, 6)){
            return 62;
        }
        //Tiefland Thüringen
        if (area == 4 || area == 7 || area == 99){
            return 71;
        }
        //Mittelgebirge Thüringen
        if (area == 8){
            return 72;
        }
        //Tiefland Sachsen
        if (isBetween(area, 2, 3)){
            return 81;
        }
        //Mittelgebirge Sachsen
        if (area == 9){
            return 82;
        }
        //Nordhessen und hess. Mittelgebirge
        if (isBetween(area, 34, 36)){
            return 91;
        }
        //Rhein-Main
        if (isBetween(area, 60, 65) || isBetween(area,68,69)){
            return 92;
        }
        //Rhein, Pfalz, Nahe und Mosel
        if (isBetween(area, 54, 56)){
            return 101;
        }
        //Mittelgebirgsbereich Rheinland-Pfalz
        if (area == 67){
            return 102;
        }
        //Saarland
        if (area == 66){
            return 103;
        }
        //Oberrhein und unteres Neckartal
        if (isBetween(area, 76, 77) || area == 79){
            return 111;
        }
        //Hohenlohe/mittlerer Neckar/Oberschwaben
        if (area == 78 || area == 88) {
            return 112;
        }
        // Mittelgebirge Baden-Württemberg
        if (isBetween(area, 70, 75)){
            return 113;
        }
        //Allgäu/Oberbayern/Bay. Wald
        if (area == 94) {
            return 121;
        }
        //Donauniederungen
        if (isBetween(area, 80, 87) || area == 89){
            return 122;
        }
        //Bayern nördl. der Donau, o. Bayr. Wald, o. Mainfranken
        if (isBetween(area, 90, 93) || isBetween(area, 95, 96) ){
            return 123;
        }
        //Mainfranken
        if (isBetween(area, 97, 98) ){
            return 124;
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
