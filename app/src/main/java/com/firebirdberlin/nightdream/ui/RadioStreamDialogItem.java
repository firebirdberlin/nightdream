/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.firebirdberlin.nightdream.ui;

import android.content.Context;

import com.firebirdberlin.nightdream.R;

import java.util.Locale;

public class RadioStreamDialogItem {

    private final String TAG = "RadioStreamDialogItem";

    private final Context context;
    private final String imageUrl;
    private final String countryCode;
    private final String name;
    private final long bitrate;
    private final boolean isOnline;
    private final boolean isCountrySelected;

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

    public long getBitrate() {
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
