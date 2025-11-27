package com.firebirdberlin.radiostreamapi.models;

import java.util.Locale;

public class Country {

    public String name;
    public String countryCode;
    public String region;
    public String subRegion;

    public void setNameFromIsoCode() {
        Locale loc = new Locale("", countryCode);
        name = loc.getDisplayCountry();
    }

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
