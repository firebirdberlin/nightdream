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
        return String.format(java.util.Locale.getDefault(),"%s (%s)\n%1.3f°; %1.3f°", name, countryCode, lat, lon);
    }

    public static City fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, City.class);
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        City city = (City) o;
        return id == city.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
