package com.firebirdberlin.nightdream;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TimePicker;

import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.receivers.WakeUpReceiver;
import com.firebirdberlin.nightdream.ui.AlarmClockLayout;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SetAlarmClockActivity extends Activity {
    static final String TAG = "SetAlarmClockActivity";
    private LinearLayout scrollView = null;
    private DataSource db = null;
    private Settings settings = null;
    private String timeFormat = "h:mm";


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
    }

    @Override
    protected void onResume() {
        super.onResume();
        settings = new Settings(this);
        timeFormat = settings.getTimeFormat();
        init();
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

        Collections.sort(entries, new Comparator<SimpleTime>() {
            public int compare(SimpleTime obj1, SimpleTime obj2) {
                return obj1.getCalendar().compareTo(obj2.getCalendar());
            }
        });


        if (!settings.is24HourFormat()) {
            timeFormat += " a";
        }
        for (SimpleTime entry : entries) {
            AlarmClockLayout layout = new AlarmClockLayout(this, entry, timeFormat);
            scrollView.addView(layout);
        }
        scrollView.invalidate();
    }

    public void onClickAddNewAlarm(View view) {
        showTimePicker(7, 0, null);
    }

    private void showTimePicker(int hour, int min, final Long entry_id) {
        final Context context = this;
        TimePickerDialog mTimePicker = new TimePickerDialog(context, R.style.DialogTheme, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                SimpleTime entry = new SimpleTime(selectedHour, selectedMinute);
                if (entry_id != null) {
                    entry.id = entry_id;
                }
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
        db.delete(entry);
        init();
        WakeUpReceiver.schedule(this);
    }

    public void onTimeClicked(View view) {
        SimpleTime entry = (SimpleTime) view.getTag();
        showTimePicker(entry.hour, entry.min, entry.id);
    }

    public void onActiveStateChanged(SimpleTime entry) {
        db.save(entry);
        WakeUpReceiver.schedule(this);
    }
}
