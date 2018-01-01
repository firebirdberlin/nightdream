package com.firebirdberlin.radiostreamapi.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class FavoriteRadioStations {

    private static final int MAX_NUM_ENTRIES = 5;

    private final RadioStation[] radioStations = new RadioStation[MAX_NUM_ENTRIES];

    public static int getMaxNumEntries() {
        return MAX_NUM_ENTRIES;
    }

    public void set(int index, RadioStation station) {
        if (index < 0 || index >= radioStations.length) {
            return;
        }

        radioStations[index] = station;
    }

    public RadioStation get(int index) {
        if (index < 0 || index >= radioStations.length) {
            return null;
        }
        return radioStations[index];
    }

    private static final String JSON_FAVORITES = "favorites";

    public static FavoriteRadioStations fromJson(String json) throws JSONException {

        FavoriteRadioStations result = new FavoriteRadioStations();

        JSONObject root = new JSONObject(json);
        JSONObject favorites = (JSONObject) root.get(JSON_FAVORITES);

        Iterator<String> keys = favorites.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject stationObj = (JSONObject) favorites.get(key);
            RadioStation station = RadioStation.fromJsonObj(stationObj);
            result.set(Integer.valueOf(key), station);
        }
        return result;
    }

    public String toJson() throws JSONException {
        JSONObject root = new JSONObject();

        JSONObject favorites = new JSONObject();
        root.put(JSON_FAVORITES, favorites);
        for (int i = 0; i < MAX_NUM_ENTRIES; i++) {
            RadioStation station = radioStations[i];
            if (station != null) {
                try {
                    favorites.put(String.valueOf(i), station.toJsonObject());
                } catch (JSONException e) {
                }
            }
        }

        return root.toString();
    }
}
