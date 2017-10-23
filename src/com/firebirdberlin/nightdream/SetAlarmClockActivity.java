package com.firebirdberlin.nightdream;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TimePicker;

import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.receivers.WakeUpReceiver;
import com.firebirdberlin.nightdream.ui.AlarmClockLayout;

import java.util.List;

public class SetAlarmClockActivity extends Activity {
    private LinearLayout scrollView = null;
    private DataSource db = null;

    public static void start(Context context) {
        Intent intent = new Intent(context, SetAlarmClockActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_alarm_clock);
        setTheme(R.style.DialogTheme);

        scrollView = (LinearLayout) findViewById(R.id.scroll_view);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        openDB();
    }

    private void openDB() {
        if (db == null) {
            db = new DataSource(this);
            db.open();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (db != null) {
            db.close();
            db = null;
        }
    }

    private void init() {
        openDB();
        scrollView.removeAllViews();
        List<SimpleTime> entries = db.getAlarms();
        for (SimpleTime entry : entries) {
            AlarmClockLayout layout = new AlarmClockLayout(this, entry);
            scrollView.addView(layout);
        }
        scrollView.invalidate();
    }

    public void onClickAddNewAlarm(View view) {
        TimePickerDialog mTimePicker;
        int hour = 7;
        int min = 0;
        final Context context = this;
        mTimePicker = new TimePickerDialog(context, R.style.DialogTheme, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                SimpleTime entry = new SimpleTime(selectedHour, selectedMinute);
                entry.isActive = true;
                db.save(entry);
                init();
                WakeUpReceiver.schedule(context);
            }
        }, hour, min, Utility.is24HourFormat(context));
        mTimePicker.show();
    }

    public void onButtonDeleteClick(View view) {
        SimpleTime entry = (SimpleTime) view.getTag();
        Log.w("SetAlarmClockActivity", String.format("button delete >>> %d", entry.id));
        db.delete(entry);
        init();
        WakeUpReceiver.schedule(this);
    }
}
