package com.firebirdberlin.openweathermapapi.models;

public class City {


    public int id = 0;
    public String name;
    public String countryCode;
    public double lat = 0.0f;
    public double lon = 0.0f;

    @Override
    public String toString() {
        return "City{" +
                "name='" + name + '\'' +
                "id='" + String.format("%d", id) + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", lat='" + String.format("%f", lat) + '\'' +
                ", lon='" + String.format("%f", lon) + '\'' +
                '}';
    }
}
