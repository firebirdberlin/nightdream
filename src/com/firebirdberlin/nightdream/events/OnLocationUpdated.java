package com.firebirdberlin.nightdream.events;

import android.location.Location;

public class OnLocationUpdated {

    public Location entry;
    public OnLocationUpdated(Location entry) {
        this.entry = entry;
    }
}
