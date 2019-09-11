package com.firebirdberlin.openweathermapapi.models;

import com.google.gson.Gson;

public class City {

    public int id = 0;
    public String name;
    public String countryCode;
    public double lat = 0.0f;
    public double lon = 0.0f;


    @Override
    public String toString() {
        return String.format("%s (%s)\n%1.3f°; %1.3f°", name, countryCode, lat, lon);
    }

    public static City fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, City.class);
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
