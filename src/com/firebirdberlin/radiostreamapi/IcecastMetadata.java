package com.firebirdberlin.radiostreamapi;

import java.util.Map;

public class IcecastMetadata {

    public final IcyHeaderInfo icyHeaderInfo;
    public final String streamTitle;
    public final boolean streamMetaDataNotSupported;

    public IcecastMetadata(IcyHeaderInfo icyHeaderInfo, String streamTitle) {
        this.icyHeaderInfo = icyHeaderInfo;
        this.streamTitle = streamTitle;
        this.streamMetaDataNotSupported = false;
    }

    public IcecastMetadata(IcyHeaderInfo icyHeaderInfo, String streamTitle, boolean streamMetaDataNotSupported) {
        this.icyHeaderInfo = icyHeaderInfo;
        this.streamTitle = streamTitle;
        this.streamMetaDataNotSupported = streamMetaDataNotSupported;
    }
}
