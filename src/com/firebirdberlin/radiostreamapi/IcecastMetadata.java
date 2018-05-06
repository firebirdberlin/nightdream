package com.firebirdberlin.radiostreamapi;

import java.util.Map;

public class IcecastMetadata {

    public final IcyHeaderInfo icyHeaderInfo;
    public final String streamTitle;
    public final boolean streamMetaDataAvailable;

    public IcecastMetadata(IcyHeaderInfo icyHeaderInfo, String streamTitle, boolean streamMetaDataAvailable) {
        this.icyHeaderInfo = icyHeaderInfo;
        this.streamTitle = streamTitle;
        this.streamMetaDataAvailable = streamMetaDataAvailable;
    }
}
