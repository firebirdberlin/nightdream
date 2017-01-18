package com.firebirdberlin.radiostreamapi.models;

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

}
