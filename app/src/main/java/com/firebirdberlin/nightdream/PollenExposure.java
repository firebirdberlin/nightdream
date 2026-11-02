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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeMap;

public class PollenExposure {

    private static final String TAG = PollenExposure.class.getSimpleName();

    // DWD publishes all times in German local time.
    private static final TimeZone DWD_TIME_ZONE = TimeZone.getTimeZone("Europe/Berlin");

    private final ArrayList<TreeMap<String, String>> pollenList = new ArrayList<>();
    private final ArrayList<HashMap<String, String>> pollenAreaList = new ArrayList<>();
    private String nextUpdate;
    private String lastUpdate;
    private String postCode = "";

    // diagnostics (see getDebugSummary())
    private int areaId = -1;
    private String areaName = "";
    private String dayKey = null;

    public PollenExposure() {}

    private static boolean isBetween(int x, int lower, int upper) {
        return lower <= x && x <= upper;
    }

    public void parse(String json, String plz) {
        parse(json, plz, System.currentTimeMillis());
    }

    /**
     * Parses the DWD file s31fg.json.
     *
     * The values "today", "tomorrow" and "dayafter_to" in the file are relative to
     * "last_update", NOT to the current day. The file is only refreshed once a day
     * (around 11:00), so before that the file's "today" is yesterday. We therefore
     * pick the field that matches the current date.
     *
     * If the JSON is broken, the previous state is kept. nowMillis is a parameter
     * to make this testable.
     */
    public void parse(String json, String plz, long nowMillis) {
        if (json == null || json.isEmpty()) {
            Log.w(TAG, "parse: no json");
            return;
        }

        final String normalizedPlz = normalizePlz(plz);
        final int area = normalizedPlz == null
                ? -1
                : plzToArea(Integer.parseInt(normalizedPlz.substring(0, 2)));

        final ArrayList<TreeMap<String, String>> newPollen = new ArrayList<>();
        final ArrayList<HashMap<String, String>> newAreas = new ArrayList<>();
        String newNext;
        String newLast;
        String newAreaName = "";
        String newDayKey;

        try {
            JSONObject root = new JSONObject(json);
            newNext = root.optString("next_update", null);
            newLast = root.optString("last_update", null);

            long lastMillis = parseDwdTime(newLast);
            if (lastMillis > 0L) {
                newDayKey = dayKeyFor(lastMillis, nowMillis); // may be null if data is too old
            } else {
                Log.w(TAG, "last_update not parseable, falling back to 'today'");
                newDayKey = "today";
            }

            JSONArray content = root.getJSONArray("content");
            for (int i = 0; i < content.length(); i++) {
                try {
                    JSONObject c = content.getJSONObject(i);
                    int regionId = c.optInt("partregion_id", -1);
                    String regionName = c.optString("partregion_name", "");
                    if (regionId == -1) {
                        regionId = c.getInt("region_id");
                        regionName = c.optString("region_name", "");
                    }

                    HashMap<String, String> areaEntry = new HashMap<>();
                    areaEntry.put(regionName, Integer.toString(regionId));
                    newAreas.add(areaEntry);

                    if (regionId == area && newDayKey != null) {
                        JSONObject pollen = c.getJSONObject("Pollen");
                        TreeMap<String, String> values = new TreeMap<>();
                        Iterator<String> it = pollen.keys();
                        while (it.hasNext()) {
                            String key = it.next();
                            JSONObject forecast = pollen.optJSONObject(key);
                            if (forecast == null) continue;
                            String value = forecast.optString(newDayKey, null);
                            if (value != null) {
                                values.put(getUnifiedHerbName(key), value);
                            }
                        }
                        if (!values.isEmpty()) {
                            newPollen.add(values);
                        }
                        newAreaName = regionName;
                    }
                } catch (JSONException e) {
                    // one broken entry must not kill the others
                    Log.e(TAG, "Json entry " + i + " error: " + e.getMessage());
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Json parsing error: " + e.getMessage());
            return;
        }

        pollenList.clear();
        pollenList.addAll(newPollen);
        pollenAreaList.clear();
        pollenAreaList.addAll(newAreas);
        nextUpdate = newNext;
        lastUpdate = newLast;
        areaId = area;
        areaName = newAreaName;
        dayKey = newDayKey;

        Log.i(TAG, getDebugSummary());
    }

    /**
     * Which field of the file describes the current day?
     * Returns null if the file is more than two days old (no usable data).
     */
    static String dayKeyFor(long lastUpdateMillis, long nowMillis) {
        Calendar last = Calendar.getInstance(DWD_TIME_ZONE);
        Calendar now = Calendar.getInstance(DWD_TIME_ZONE);
        last.setTimeInMillis(lastUpdateMillis);
        now.setTimeInMillis(nowMillis);
        toNoon(last);
        toNoon(now);
        // round() keeps this correct across DST changes
        long days = Math.round((now.getTimeInMillis() - last.getTimeInMillis()) / 86400000.0);
        if (days <= 0) return "today"; // also covers a device clock that is slightly behind
        if (days == 1) return "tomorrow";
        if (days == 2) return "dayafter_to";
        return null;
    }

    private static void toNoon(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    /** Parses strings like "2026-11-02 11:00 Uhr" (German local time). */
    static long parseDwdTime(String s) {
        if (s == null || s.length() < 16) {
            return -1L;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        format.setTimeZone(DWD_TIME_ZONE);
        try {
            return format.parse(s.substring(0, 16)).getTime();
        } catch (ParseException e) {
            Log.w(TAG, "cannot parse time: " + s);
            return -1L;
        }
    }

    /** Returns the 5 digit postal code or null. Accepts e.g. "D-10115" or " 10115 ". */
    static String normalizePlz(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("\\D", "");
        return digits.length() == 5 ? digits : null;
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

    /**
     * Maps the first two digits of a German postal code to a DWD pollen area.
     * This is only an approximation (see TODOs). A 5 digit table would be better.
     * Returns -1 if there is no mapping.
     */
    private Integer plzToArea(Integer p) {
        if (isBetween(p, 25, 25)) {
            //Inseln und Marschen
            return 11;
        } else if (isBetween(p, 20, 24)) {
            //Geest, Schleswig-Holstein und Hamburg plz 20 -24
            return 12;
        } else if (isBetween(p, 17, 19)) {
            //Mecklenburg-Vorpommern
            return 20;
        } else if (isBetween(p, 26, 29)) {
            //Westl. Niedersachsen und Bremen
            return 31;
        } else if (isBetween(p, 30, 31) || isBetween(p, 37, 38)) {
            //Östl. Niedersachsen
            return 32;
        } else if (isBetween(p, 40, 52)) {
            //Rhein.-Westfäl. Tiefland
            return 41;
        } else if (isBetween(p, 32, 33) || isBetween(p, 57, 59)) {
            //Ostwestfalen
            return 42;
        } else
        if (p == 53) {
            //Mittelgebirge NRW
            return 43;
        } else if (isBetween(p, 10, 16)) {
            //Brandenburg und Berlin
            return 50;
        } else if (p == 39) {
            //Tiefland Sachsen-Anhalt
            return 61;
        } else if (p == 6) {
            //Harz
            return 62;
        } else if (p == 4 || p == 7 || p == 99) {
            //Tiefland Thüringen
            return 71;
        } else if (p == 8) {
            //Mittelgebirge Thüringen
            return 72;
        } else if (isBetween(p, 2, 3)) {
            //Tiefland Sachsen
            return 81;
        } else if (p == 9) {
            //Mittelgebirge Sachsen
            return 82;
        } else if (isBetween(p, 34, 36)) {
            //Nordhessen und hess. Mittelgebirge
            return 91;
        } else if (isBetween(p, 60, 65) || isBetween(p, 68, 69)) {
            //Rhein-Main
            return 92;
        } else if (isBetween(p, 54, 56)) {
            //Rhein, Pfalz, Nahe und Mosel
            return 101;
        } else if (p == 67) {
            //Mittelgebirgsbereich Rheinland-Pfalz
            return 102;
        } else if (p == 66) {
            //Saarland
            return 103;
        } else if (isBetween(p, 76, 77) || p == 79) {
            //Oberrhein und unteres Neckartal
            return 111;
        } else if (p == 78 || p == 88) {
            //Hohenlohe/mittlerer Neckar/Oberschwaben
            return 112;
        } else if (isBetween(p, 70, 75)) {
            // Mittelgebirge Baden-Württemberg
            return 113;
        } else if (p == 94) {
            //Allgäu/Oberbayern/Bay. Wald
            return 121;
        } else if (isBetween(p, 80, 87) || p == 89) {
            //Donauniederungen
            return 122;
        } else if (isBetween(p, 90, 93) || isBetween(p, 95, 96)) {
            //Bayern nördl. der Donau, o. Bayr. Wald, o. Mainfranken
            return 123;
        } else if (isBetween(p, 97, 98)) {
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
        return parseDwdTime(nextUpdate);
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public int getAreaId() {
        return areaId;
    }

    /** true as soon as DWD should have published a new file. */
    public boolean isExpired() {
        long next = getNextUpdate();
        return next != -1L && System.currentTimeMillis() > next;
    }
    public void setPostCode(String postCode) {
        String newCode = postCode == null ? "" : postCode;
        if (!newCode.equals(this.postCode)) {
            nextUpdate = null;
            lastUpdate = null;
            areaId = -1;
            areaName = "";
            dayKey = null;
            this.pollenList.clear();
            this.pollenAreaList.clear();
        }
        this.postCode = newCode;
    }

    public String getDebugSummary() {
        return "plz=" + postCode
                + " area=" + areaId + " (" + areaName + ")"
                + " dayKey=" + dayKey
                + " last_update=" + lastUpdate
                + " next_update=" + nextUpdate
                + " herbs=" + (pollenList.isEmpty() ? 0 : pollenList.get(0).size())
                + " areas=" + pollenAreaList.size();
    }
}