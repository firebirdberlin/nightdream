package com.firebirdberlin.nightdream;

import android.animation.LayoutTransition;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.AlarmClock;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatDelegate;

import com.firebirdberlin.nightdream.events.OnAlarmEntryChanged;
import com.firebirdberlin.nightdream.events.OnAlarmEntryDeleted;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.receivers.WakeUpReceiver;
import com.firebirdberlin.nightdream.services.AlarmNotificationService;
import com.firebirdberlin.nightdream.services.SqliteIntentService;
import com.firebirdberlin.nightdream.ui.AlarmClockLayout;
import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class SetAlarmClockActivity extends BillingHelperActivity {
    static final String TAG = "SetAlarmClockActivity";
    private LinearLayout scrollView = null;
    private DataSource db = null;
    private String timeFormat = "h:mm";
    private String dateFormat = "h:mm";
    private List<SimpleTime> entries = null;
    private HashMap<Long, AlarmClockLayout> layoutHashMap = new HashMap<>();
    private FavoriteRadioStations radioStations = null;

    public static void start(Context context) {
        Intent intent = new Intent(context, SetAlarmClockActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_set_alarm_clock);
        setTheme(R.style.AlarmClockActivityTheme);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        scrollView = findViewById(R.id.scroll_view);
        // https://www.youtube.com/watch?v=55wLsaWpQ4g
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            LayoutTransition layoutTransition = scrollView.getLayoutTransition();
            layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Utility.registerEventBus(this);
        Settings settings = new Settings(this);
        timeFormat = settings.getFullTimeFormat();
        dateFormat = settings.dateFormat;
        radioStations = settings.getFavoriteRadioStations();
        init();

        Intent intent = getIntent();
        Utility.logIntent(TAG, "onResume()", intent);
        if (intent != null && AlarmClock.ACTION_SET_ALARM.equals(intent.getAction())) {
            intent.setAction(""); // clear that intent

            saveAlarmEntryFromIntent(intent);
        }
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        Utility.unregisterEventBus(this);
    }

    private void init() {
        openDB();
        if (entries == null) {
            entries = db.getAlarms();
        }
        update();
    }

    private void update() {
        update(null);
    }

    private void update(Long highlight_entry_id) {
        Collections.sort(entries, new Comparator<SimpleTime>() {
            public int compare(SimpleTime obj1, SimpleTime obj2) {
                return obj1.getCalendar().compareTo(obj2.getCalendar());
            }
        });
        scrollView.removeAllViews();
        for (SimpleTime entry : entries) {
            AlarmClockLayout layout = layoutHashMap.get(entry.id);

            if (layout == null) {
                layout = new AlarmClockLayout(this, entry, timeFormat, dateFormat,
                        radioStations);
                layoutHashMap.put(entry.id, layout);
            }

            if (highlight_entry_id != null && entry.id == highlight_entry_id) {
                layout.showSecondaryLayout(true);
            }
            scrollView.addView(layout);
            layout.update();
        }
        scrollView.invalidate();
    }

    public void onClickAddNewAlarm(View view) {
        showTimePicker(7, 0, null);
    }

    private void showTimePicker(int hour, int min, final Long entry_id) {
        final Context context = this;
        TimePickerDialog mTimePicker = new TimePickerDialog(
                context,
               // R.style.DialogTheme,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        // Bug Android 4.1: Dialog is submitted twice
                        // >> ignore second call to this method.
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                                && !timePicker.isShown()) return;

                        SimpleTime entry = null;
                        boolean isNew = (entry_id == null);
                        if (isNew) {
                            entry = new SimpleTime();
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
                                entry.autocompleteRecurringDays();
                            }
                            entry = db.save(entry);
                            if (entry_id == null) {
                                entries.add(entry);
                                update(entry.id);
                            } else {
                                update();
                            }

                        }
                        WakeUpReceiver.schedule(context, db);
                    }
                },
            hour, min, Utility.is24HourFormat(context));
        // fix broken dialog appearance on some devices
        mTimePicker.setTitle(null);
        mTimePicker.show();
    }

    private void showDatePicker(long timestamp, final Long entry_id) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        final Context context = this;
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog mDatePicker = new DatePickerDialog(context, R.style.DialogTheme, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                // Bug Android 4.1: Dialog is submitted twice
                // >> ignore second call to this method.
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                        && !view.isShown()) return;

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, month);
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                db.updateNextEventAfter(entry_id, cal.getTimeInMillis());
                SqliteIntentService.scheduleAlarm(context);
            }
        }, year, month, dayOfMonth);
        mDatePicker.setTitle(R.string.alarmStartDate);
        mDatePicker.show();
    }

    public void onButtonDeleteClick(View view) {
        SimpleTime entry = (SimpleTime) view.getTag();
        db.delete(entry);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmNotificationService.cancelNotification(this);
        }
        WakeUpReceiver.schedule(this, db);
    }

    public void onTimeClicked(View view) {
        SimpleTime entry = (SimpleTime) view.getTag();
        showTimePicker(entry.hour, entry.min, entry.id);
    }

    public void onDateClicked(View view) {
        SimpleTime entry = (SimpleTime) view.getTag();
        if (!entry.isRecurring()) {
            return;
        }
        Long nextEventAfter = entry.nextEventAfter;
        if (nextEventAfter == null || nextEventAfter == 0L) {
            nextEventAfter = entry.getCalendar().getTimeInMillis();
        }
        showDatePicker(nextEventAfter, entry.id);
    }

    public void onEntryStateChanged(SimpleTime entry) {
        db.save(entry);
        SqliteIntentService.scheduleAlarm(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAlarmEntryChanged(OnAlarmEntryChanged event) {
        Log.d(TAG, "onAlarmEntryChanged");
        AlarmClockLayout layout = layoutHashMap.get(event.entry.id);
        if (layout != null) {
            layout.updateAlarmClockEntry(event.entry);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAlarmEntryDeleted(OnAlarmEntryDeleted event) {
        Log.d(TAG, "onAlarmEntryDeleted");
        int id = -1;
        for (int i = 0; i < entries.size(); i++) {
            SimpleTime entry = entries.get(i);
            if (event.entry.id == entry.id) {
                id = i;
                break;
            }
        }
        if (id > -1) entries.remove(id);
        update();
    }

    private void saveAlarmEntryFromIntent(Intent intent) {
        if (intent == null) return;
        SimpleTime entry = new SimpleTime();

        entry.hour = intent.getIntExtra(AlarmClock.EXTRA_HOUR, 0);
        entry.min = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, 0);

        ArrayList<Integer> days = intent.getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS);
        if (days != null) {
            for (int day: days) {
                entry.addRecurringDay(day);
            }
        }
        entry.isActive = true;
        entry = db.save(entry);
        entries.add(entry);
        update(entry.id);
        if (intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 3000);
        }
    }
}
