package com.firebirdberlin.nightdream.events;

import com.firebirdberlin.nightdream.models.SimpleTime;

public class OnAlarmEntryDeleted {

    public SimpleTime entry;

    public OnAlarmEntryDeleted(SimpleTime entry) {
        this.entry = entry;
    }

}
