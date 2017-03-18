package com.firebirdberlin.radiostreamapi.models;

import org.json.JSONException;
import org.json.JSONObject;

public class RadioStation {

    public long id;
    public String name;
    public String countryCode;
    public String stream;
    public boolean isOnline;
    public long bitrate;

    public String toString() {
        return String.format("%s %s (%d kbit/s)", this.countryCode, this.name, this.bitrate);
    }

    private static final String JSON_ID = "id";
    private static final String JSON_NAME = "name";
    private static final String JSON_COUNTRY = "country";
    private static final String JSON_STREAM = "stream";
    private static final String JSON_BITRATE = "bitrate";
    private static final String JSON_STATUS = "status";

    public static RadioStation fromJson(String json) throws JSONException {
        JSONObject jsonStation = new JSONObject(json);
        RadioStation station = new RadioStation();
        station.id = jsonStation.getLong(JSON_ID);
        station.name = jsonStation.getString(JSON_NAME);
        station.countryCode = jsonStation.getString(JSON_COUNTRY);
        station.stream = jsonStation.getString(JSON_STREAM);
        String bitRateString = jsonStation.getString(JSON_BITRATE);
        if (bitRateString != null && !bitRateString.isEmpty()) {
            try {
                station.bitrate = jsonStation.getLong(JSON_BITRATE);
            } catch (Throwable t) {

            }
        }

        //station.isOnline = jsonStation.getLong(JSON_STATUS) == 1L;

        return station;
    }

    public String toJson() throws JSONException {
        JSONObject jsonStation = new JSONObject();
        jsonStation.put(JSON_ID, id);
        jsonStation.put(JSON_NAME, name);
        jsonStation.put(JSON_COUNTRY, countryCode);
        jsonStation.put(JSON_STREAM, stream);
        jsonStation.put(JSON_BITRATE, bitrate);
        //jsonStation.put(JSON_STATUS, isOnline);

        return jsonStation.toString();
    }
}
