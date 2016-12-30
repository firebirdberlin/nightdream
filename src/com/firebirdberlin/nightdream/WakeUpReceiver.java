package com.firebirdberlin.nightdream;

import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.firebirdberlin.nightdream.services.AlarmService;

public class WakeUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmService.startAlarm(context);
        Bundle alarm = new Bundle();
        alarm.putString("action", "start alarm");
        NightDreamActivity.start(context, alarm);
    }
}
