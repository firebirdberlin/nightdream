package com.firebirdberlin.nightdream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class SetAlarmClockActivity extends Activity {

    public static void start(Context context) {
        Intent intent = new Intent(context, SetAlarmClockActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_alarm_clock);
    }
}
