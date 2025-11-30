/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.firebirdberlin.nightdream;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeMap;

public class PollenExposure {

    private static String TAG = "PollenObject";

    private final ArrayList<TreeMap<String, String>> pollenList = new ArrayList<>();
    private final ArrayList<HashMap<String, String>> pollenAreaList = new ArrayList<>();
    private String nextUpdate;
    private String postCode = "";

    public PollenExposure() {}

    private static boolean isBetween(int x, int lower, int upper) {
        return lower <= x && x <= upper;
    }

    public void parse(String json, String plz) {
        pollenList.clear();
        pollenAreaList.clear();

        Integer area = -1;
        try {
            area = plzToArea(Integer.parseInt(plz.substring(0, 2)));
        } catch (NumberFormatException ignored) { }

        if (json != null && area != -1) {
            try {
                JSONObject jsonObj = new JSONObject(json);
                this.nextUpdate = jsonObj.getString("next_update");

                JSONArray content = jsonObj.getJSONArray("content");

                for (int i = 0; i < content.length(); i++) {
                    JSONObject c = content.getJSONObject(i);
                    String region_name = c.getString("partregion_name");
                    int region_ID = c.getInt("partregion_id");

                    if (region_ID == -1) {
                        region_ID = c.getInt("region_id");
                        region_name = c.getString("region_name");
                    }

                    HashMap<String, String> pollenAreaTmp = new HashMap<>();
                    pollenAreaTmp.put(region_name, Integer.toString(region_ID));
                    pollenAreaList.add(pollenAreaTmp);

                    if (region_ID == area) {
                        JSONObject pollen = c.getJSONObject("Pollen");

                        TreeMap<String, String> pollenTmp = new TreeMap<>();

                        Iterator<String> iter = pollen.keys();
                        while (iter.hasNext()) {
                            String key = iter.next();
                            try {
                                String herb = getUnifiedHerbName(key);
                                JSONObject forecast = pollen.getJSONObject(key);
                                pollenTmp.put(herb, forecast.getString("today"));
                            } catch (JSONException e) {
                                Log.e(TAG, "Json pollen error: " + e.getMessage());
                            }
                        }
                        pollenList.add(pollenTmp);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "Json parsing error: " + e.getMessage());
            }
        }
    }

    private String getUnifiedHerbName(String key) {
        switch (key) {
            case "Ambrosia":
                return "ambrosia";
            case "Beifuss":
                return "mugwort";
            case "Birke":
                return "birch";
            case "Erle":
                return "alder";
            case "Esche":
                return "ash";
            case "Graeser":
                return "grass";
            case "Hasel":
                return "hazelnut";
            case "Roggen":
                return "rye";
        }
        return key;
    }

    private Integer plzToArea(Integer area) {
        if (isBetween(area, 25, 25)) {
            //Inseln und Marschen
            return 11;
        } else if (isBetween(area, 20, 24)) {
            //Geest, Schleswig-Holstein und Hamburg plz 20 -24
            return 12;
        } else if (isBetween(area, 17, 19)) {
            //Mecklenburg-Vorpommern
            return 20;
        } else if (isBetween(area, 26, 29)) {
            //Westl. Niedersachsen und Bremen
            return 31;
        } else if (isBetween(area, 30, 31) || isBetween(area, 37, 38)) {
            //Östl. Niedersachsen
            return 32;
        } else if (isBetween(area, 40, 52)) {
            //Rhein.-Westfäl. Tiefland
            return 41;
        } else if (isBetween(area, 32, 33) || isBetween(area, 57, 59)) {
            //Ostwestfalen
            return 42;
        } else
        if (area == 53) {
            //Mittelgebirge NRW
            return 43;
        } else if (isBetween(area, 10, 16)) {
            //Brandenburg und Berlin
            return 50;
        } else if (area == 39) {
            //Tiefland Sachsen-Anhalt
            return 61;
        } else if (area == 6) {
            //Harz
            return 62;
        } else if (area == 4 || area == 7 || area == 99) {
            //Tiefland Thüringen
            return 71;
        } else if (area == 8) {
            //Mittelgebirge Thüringen
            return 72;
        } else if (isBetween(area, 2, 3)) {
            //Tiefland Sachsen
            return 81;
        } else if (area == 9) {
            //Mittelgebirge Sachsen
            return 82;
        } else if (isBetween(area, 34, 36)) {
            //Nordhessen und hess. Mittelgebirge
            return 91;
        } else if (isBetween(area, 60, 65) || isBetween(area, 68, 69)) {
            //Rhein-Main
            return 92;
        } else if (isBetween(area, 54, 56)) {
            //Rhein, Pfalz, Nahe und Mosel
            return 101;
        } else if (area == 67) {
            //Mittelgebirgsbereich Rheinland-Pfalz
            return 102;
        } else if (area == 66) {
            //Saarland
            return 103;
        } else if (isBetween(area, 76, 77) || area == 79) {
            //Oberrhein und unteres Neckartal
            return 111;
        } else if (area == 78 || area == 88) {
            //Hohenlohe/mittlerer Neckar/Oberschwaben
            return 112;
        } else if (isBetween(area, 70, 75)) {
            // Mittelgebirge Baden-Württemberg
            return 113;
        } else if (area == 94) {
            //Allgäu/Oberbayern/Bay. Wald
            return 121;
        } else if (isBetween(area, 80, 87) || area == 89) {
            //Donauniederungen
            return 122;
        } else if (isBetween(area, 90, 93) || isBetween(area, 95, 96)) {
            //Bayern nördl. der Donau, o. Bayr. Wald, o. Mainfranken
            return 123;
        } else if (isBetween(area, 97, 98)) {
            //Mainfranken
            return 124;
        }
        return -1;
    }

    public ArrayList<TreeMap<String, String>> getPollenList() {
        return this.pollenList;
    }

    public ArrayList<HashMap<String, String>> getPollenAreaList() {
        return this.pollenAreaList;
    }

    public long getNextUpdate() {
        if (nextUpdate == null) {
            return -1L;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        try {
            Date next = dateFormat.parse(nextUpdate);
            Log.i(TAG, "next: " + next.toString());
            return next.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return -1L;
        }
    }

    public boolean isValid() {
        long nextUpdate = getNextUpdate();
        long now = System.currentTimeMillis();

        if (nextUpdate == -1L) {
            return false;
        }
        return  now > nextUpdate;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        if (postCode == null || !postCode.equals(this.postCode)) {
            nextUpdate = null;
            this.pollenList.clear();
            this.pollenAreaList.clear();
        }
        this.postCode = postCode;

    }
}
