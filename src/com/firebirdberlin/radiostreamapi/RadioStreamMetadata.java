package com.firebirdberlin.radiostreamapi;

public class RadioStreamMetadata {

    public final IcyHeaderInfo icyHeaderInfo;
    public final String streamTitle;
    public final boolean streamMetaDataNotSupported;

    public RadioStreamMetadata(IcyHeaderInfo icyHeaderInfo, String streamTitle) {
        this.icyHeaderInfo = icyHeaderInfo;
        this.streamTitle = streamTitle;
        this.streamMetaDataNotSupported = false;
    }

    public RadioStreamMetadata(IcyHeaderInfo icyHeaderInfo, String streamTitle, boolean streamMetaDataNotSupported) {
        this.icyHeaderInfo = icyHeaderInfo;
        this.streamTitle = streamTitle;
        this.streamMetaDataNotSupported = streamMetaDataNotSupported;
    }
}
