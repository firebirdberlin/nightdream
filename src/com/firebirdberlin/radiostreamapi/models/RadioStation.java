package com.firebirdberlin.radiostreamapi.models;

import org.json.JSONException;
import org.json.JSONObject;

public class RadioStation {

    private static final String JSON_ID = "id";
    private static final String JSON_NAME = "name";
    private static final String JSON_COUNTRY = "country";
    private static final String JSON_STREAM = "stream";
    private static final String JSON_BITRATE = "bitrate";
    private static final String JSON_USER_DEFINED_STREAM_URL = "user_defined_stream_URL";
    private static final String JSON_STATUS = "status";
    public long id;
    public String name;
    public String countryCode;
    public String stream;
    public boolean isOnline;
    public long bitrate;
    public boolean isUserDefinedStreamUrl = false;

    public static RadioStation fromJson(String json) throws JSONException {
        JSONObject jsonStation = new JSONObject(json);
        return fromJsonObj(jsonStation);
    }

    public static RadioStation fromJsonObj(JSONObject jsonStation) throws JSONException {
        RadioStation station = new RadioStation();
        station.id = jsonStation.getLong(JSON_ID);
        station.name = jsonStation.getString(JSON_NAME);
        station.countryCode = jsonStation.getString(JSON_COUNTRY);
        station.stream = jsonStation.getString(JSON_STREAM);
        try {
            station.isUserDefinedStreamUrl = jsonStation.getBoolean(JSON_USER_DEFINED_STREAM_URL);
        } catch (JSONException e) {
            station.isUserDefinedStreamUrl = false;
        }
        String bitRateString = jsonStation.getString(JSON_BITRATE);

        if (bitRateString != null && !bitRateString.isEmpty()) {
            try {
                station.bitrate = jsonStation.getLong(JSON_BITRATE);
            } catch (JSONException e) {
            }
        }

        //station.isOnline = jsonStation.getLong(JSON_STATUS) == 1L;

        return station;
    }

    public String toString() {
        return String.format("%s %s (%d kbit/s)", this.countryCode, this.name, this.bitrate);
    }

    public String toJson() throws JSONException {
        final JSONObject jsonStation = toJsonObject();
        return jsonStation.toString();
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject jsonStation = new JSONObject();
        jsonStation.put(JSON_ID, id);
        jsonStation.put(JSON_NAME, name);
        jsonStation.put(JSON_COUNTRY, countryCode);
        jsonStation.put(JSON_STREAM, stream);
        jsonStation.put(JSON_BITRATE, bitrate);
        jsonStation.put(JSON_USER_DEFINED_STREAM_URL, isUserDefinedStreamUrl);
        //jsonStation.put(JSON_STATUS, isOnline);

        return jsonStation;
    }
}
