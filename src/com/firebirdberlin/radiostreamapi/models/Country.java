package com.firebirdberlin.radiostreamapi.models;

public class Country {

    public String name;
    public String countryCode;
    public String region;
    public String subRegion;

    @Override
    public String toString() {
        return "Country{" +
                "name='" + name + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", region='" + region + '\'' +
                ", subRegion='" + subRegion + '\'' +
                '}';
    }
}
