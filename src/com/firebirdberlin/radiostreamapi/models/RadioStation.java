package com.firebirdberlin.radiostreamapi.models;

public class RadioStation {

    public long id;
    public String name;
    public String countryCode;
    public String stream;

    public String toString() {
        return String.format("%s %s", this.countryCode, this.name);
    }

}
