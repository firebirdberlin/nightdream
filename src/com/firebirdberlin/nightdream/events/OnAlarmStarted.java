package com.firebirdberlin.nightdream.events;

import com.firebirdberlin.nightdream.models.SimpleTime;

public class OnAlarmStarted {

    public SimpleTime entry;

    public OnAlarmStarted(SimpleTime entry) {
        this.entry = entry;
    }

}
