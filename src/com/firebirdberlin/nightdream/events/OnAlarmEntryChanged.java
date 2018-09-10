package com.firebirdberlin.nightdream.events;

import com.firebirdberlin.nightdream.models.SimpleTime;

public class OnAlarmEntryChanged {

    public SimpleTime entry;

    public OnAlarmEntryChanged(SimpleTime entry) {
        this.entry = entry;
    }

}
