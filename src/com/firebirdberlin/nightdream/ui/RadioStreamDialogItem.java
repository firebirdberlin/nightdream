package com.firebirdberlin.nightdream.ui;

import android.content.Context;

import com.firebirdberlin.nightdream.R;
import java.util.Locale;

public class RadioStreamDialogItem {

    private final String TAG = "RadioStreamDialogItem";

    private Context context;
    private String imageUrl;
    private String countryCode;
    private String name;
    private long bitrate;
    private boolean isOnline;
    private boolean isCountrySelected;

    public RadioStreamDialogItem(Context context, String imageUrl, String countryCode, String name, long bitrate, boolean isOnline, boolean isCountrySelected) {
        this.context = context;
        this.imageUrl = imageUrl;
        this.countryCode = countryCode;
        this.name = name;
        this.bitrate = bitrate;
        this.isOnline = isOnline;
        this.isCountrySelected = isCountrySelected;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public String getName() {
        return this.name;
    }

    public long getBitrate(){
        return this.bitrate;
    }

    public Boolean getIsOnline() {
        return this.isOnline;
    }

    public Boolean getIsCountrySelected() {
        return this.isCountrySelected;
    }

    @Override
    public String toString() {
           // String countryCode = (displayCountryCode) ? String.format("%s ", this.countryCode) : "";
            String streamOffline =
                    (this.isOnline)
                            ? ""
                            : String.format(" - %s", this.context.getResources().getString(R.string.radio_stream_offline));
            return String.format(Locale.getDefault(), "%s %s (%d kbit/s) %s",
                    this.countryCode, this.name, this.bitrate, streamOffline);
    }

}
