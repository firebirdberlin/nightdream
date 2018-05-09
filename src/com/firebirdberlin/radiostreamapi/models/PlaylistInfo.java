package com.firebirdberlin.radiostreamapi.models;


public class PlaylistInfo {

    public enum Format {M3U, PLS, ASHX, ASX};

    public enum Error {INVALID_URL, INVALID_CONTENT, UNREACHABLE_URL, UNSUPPORTED_FORMAT}

    public boolean valid = true;

    public Error error = null;

    public String streamUrl;

    public String description;

    public Format format;

    public Integer bitrateHint;
}
