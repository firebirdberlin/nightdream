package com.firebirdberlin.nightdream;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TimePicker;

import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.receivers.WakeUpReceiver;
import com.firebirdberlin.nightdream.ui.AlarmClockLayout;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SetAlarmClockActivity extends Activity {
    static final String TAG = "SetAlarmClockActivity";
    private LinearLayout scrollView = null;
    private DataSource db = null;
    private Settings settings = null;
    private String timeFormat = "h:mm";
    private List<SimpleTime> entries = null;

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
        // https://www.youtube.com/watch?v=55wLsaWpQ4g
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            LayoutTransition layoutTransition = scrollView.getLayoutTransition();
            layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        settings = new Settings(this);
        timeFormat = settings.getTimeFormat();
        if (!settings.is24HourFormat()) {
            timeFormat += " a";
        }
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
        entries = db.getAlarms();
        update();
    }

    private void update() {
        Collections.sort(entries, new Comparator<SimpleTime>() {
            public int compare(SimpleTime obj1, SimpleTime obj2) {
                return obj1.getCalendar().compareTo(obj2.getCalendar());
            }
        });
        scrollView.removeAllViews();
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
                // Bug Android 4.1: Dialog is submitted twice
                // >> ignore second call to this method.
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                        && !timePicker.isShown()) return;

                SimpleTime entry = null;
                boolean isNew = false;
                if (entry_id == null) {
                    entry = new SimpleTime();
                    isNew = true;
                } else {
                    for (SimpleTime e : entries) {
                        if (e.id == entry_id) {
                            entry = e;
                            break;
                        }
                    }
                }
                if (entry != null) {
                    entry.hour = selectedHour;
                    entry.min = selectedMinute;
                    entry.isActive = true;
                    if (isNew) {
                        entry.addRecurringDay(Calendar.MONDAY);
                        entry.addRecurringDay(Calendar.TUESDAY);
                        entry.addRecurringDay(Calendar.WEDNESDAY);
                        entry.addRecurringDay(Calendar.THURSDAY);
                        entry.addRecurringDay(Calendar.FRIDAY);
                    }
                    db.save(entry);
                    if (entry_id == null) {
                        entries.add(entry);
                    }
                }
                update();
                WakeUpReceiver.schedule(context, db);
            }
        }, hour, min, Utility.is24HourFormat(context));
        mTimePicker.show();
    }

    public void onButtonDeleteClick(View view) {
        SimpleTime entry = (SimpleTime) view.getTag();
        db.delete(entry);
        entries.remove(entry);
        update();
        WakeUpReceiver.schedule(this, db);
    }

    public void onTimeClicked(View view) {
        SimpleTime entry = (SimpleTime) view.getTag();
        showTimePicker(entry.hour, entry.min, entry.id);
    }

    public void onEntryStateChanged(SimpleTime entry) {
        db.save(entry);
        WakeUpReceiver.schedule(this);
    }
}
