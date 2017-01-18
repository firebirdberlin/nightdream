package com.firebirdberlin.nightdream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.firebirdberlin.nightdream.receivers.WakeUpReceiver;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerConnectionReceiver.schedule(context);
        WakeUpReceiver.schedule(context);
    }
}
