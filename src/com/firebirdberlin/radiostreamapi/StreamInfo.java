package com.firebirdberlin.radiostreamapi;

public class StreamInfo {

    public final boolean available;
    public final IcyHeaderInfo icyHeaderInfo;

    public StreamInfo(boolean available, IcyHeaderInfo icyHeaderInfo) {
        this.available = available;
        this.icyHeaderInfo = icyHeaderInfo;
    }
}
