package com.firebirdberlin.radiostreamapi.models;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class FavoriteRadioStations {

    public static final int MAX_NUM_ENTRIES = 6;

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

    public int numAvailableStations() {
        int total = 0;
        for (RadioStation radioStation : radioStations) {
            if (radioStation != null) {
                total++;
            }
        }
        return total;
    }

    public int previousAvailableIndex(int currentIndex) {
        if (currentIndex < 0 || currentIndex >= radioStations.length) {
            return -1;
        }

        int nextIndex = currentIndex - 1;

        if (nextIndex < 0) {
            for (nextIndex = radioStations.length - 1; nextIndex > 0; nextIndex--) {
                RadioStation s = radioStations[nextIndex];
                if (s != null) {
                    return nextIndex;
                }
            }
        } else {
            return nextIndex;
        }

        // should never happen
        return -1;
    }

    public int nextAvailableIndex(int currentIndex) {
        if (currentIndex < 0 || currentIndex >= radioStations.length) {
            return -1;
        }

        for (int i = 0; i < radioStations.length; i++) {
            int nextIndex = (i + currentIndex + 1) % radioStations.length;
            RadioStation s = radioStations[nextIndex];
            if (s != null) {
                return nextIndex;
            }
        }
        // should never happen
        return -1;
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
            result.set(Integer.parseInt(key), station);
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
                } catch (JSONException ignored) {
                }
            }
        }

        return root.toString();
    }
}
