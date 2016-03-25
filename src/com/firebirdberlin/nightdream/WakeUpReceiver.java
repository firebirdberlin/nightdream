package com.firebirdberlin.nightdream;
import android.app.Activity;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;
import android.content.SharedPreferences;

public class WakeUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle alarm = new Bundle();
        alarm.putString("what", "alarm");
        alarm.putString("action", "start");
        NightDreamActivity.start(context, alarm);
    }
}
