package com.firebirdberlin.nightdream;

import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.services.AlarmService;
import com.firebirdberlin.nightdream.services.RadioStreamService;

public class WakeUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Settings settings = new Settings(context);
        if (settings.useInternalAlarm ) {
            if ( settings.useRadioAlarmClock && Utility.hasNetworkConnection(context) ) {
                RadioStreamService.start(context);
            } else {
                AlarmService.startAlarm(context);
            }

            NightDreamActivity.start(context);
        }
    }
}
