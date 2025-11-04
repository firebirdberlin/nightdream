package com.firebirdberlin.nightdream;

import android.animation.LayoutTransition;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.AlarmClock;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.firebirdberlin.nightdream.events.OnAlarmEntryChanged;
import com.firebirdberlin.nightdream.events.OnAlarmEntryDeleted;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.receivers.WakeUpReceiver;
import com.firebirdberlin.nightdream.services.AlarmNotificationService;
import com.firebirdberlin.nightdream.services.SqliteIntentService;
import com.firebirdberlin.nightdream.ui.AlarmClockLayout;
import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class SetAlarmClockActivity extends BillingHelperActivity {
    static final String TAG = "SetAlarmClockActivity";

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }

    final int MENU_ITEM_PREFERENCES = 3000;
    private final HashMap<Long, AlarmClockLayout> layoutHashMap = new HashMap<>();
    private LinearLayout scrollView = null;
    private DataSource db = null;
    private String timeFormat = "h:mm";
    private String dateFormat = "h:mm";
    private List<SimpleTime> entries = null;
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

        scrollView = findViewById(R.id.scroll_view);

        // https://www.youtube.com/watch?v=55wLsaWpQ4g
        LayoutTransition layoutTransition = scrollView.getLayoutTransition();
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // return super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MENU_ITEM_PREFERENCES, Menu.NONE, getString(R.string.preferences))
                .setIcon(R.drawable.ic_settings)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //return super.onOptionsItemSelected(item);

        if (item.getItemId() == MENU_ITEM_PREFERENCES) {
            AlarmsPreferenceActivity.start(this);
            return true;
        }
        return false;
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
        if (AlarmClock.ACTION_SET_ALARM.equals(intent.getAction())) {
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
        Collections.sort(entries, (obj1, obj2) -> obj1.getCalendar().compareTo(obj2.getCalendar()));
        scrollView.removeAllViews();
        for (SimpleTime entry : entries) {
            AlarmClockLayout layout = layoutHashMap.get(entry.id);

            if (layout == null) {
                layout = new AlarmClockLayout(
                        this, entry, timeFormat, dateFormat, radioStations
                );
                layoutHashMap.put(entry.id, layout);
            }

            if (highlight_entry_id != null && entry.id == highlight_entry_id) {
                layout.showSecondaryLayout(true);
            }
            scrollView.addView(layout);
        }
        scrollView.invalidate();
    }

    public void onClickAddNewAlarm(View view) {
        Log.d(TAG, "onClickAddNewAlarm()");
        newAlarm(view);
    }

    public void newAlarm(View view) {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int min;
        boolean recurring = true;
        if (hour > 9 && hour < 18) {
            // short
            now.add(Calendar.MINUTE, 30);
            hour = now.get(Calendar.HOUR_OF_DAY);
            min = (now.get(Calendar.MINUTE) / 10 + 1) * 10;
            if (min >= 60) {
                min = 0;
                hour = (hour + 1 > 23) ? 0 : hour + 1;
            }
            recurring = false;
        } else {
            // long
            hour = 7;
            min = 0;
        }
        showTimePicker(hour, min, recurring, null);
    }

    private void showTimePicker(int hour, int min, boolean recurring, final Long entry_id) {
        final Context context = this;
        TimePickerDialog mTimePicker = new TimePickerDialog(
                context, R.style.DialogTheme,
                (timePicker, selectedHour, selectedMinute) -> {
                    // Bug Android 4.1: Dialog is submitted twice
                    // >> ignore second call to this method.
                    if (!timePicker.isShown()) return;

                    SimpleTime entry = null;
                    boolean isNew = (entry_id == null);
                    if (isNew) {
                        entry = new SimpleTime();
                        entry.name = getResources().getString(R.string.alarm);
                        entry.soundUri = Settings.getDefaultAlarmTone(context);
                        entry.radioStationIndex = Settings.getDefaultRadioStation(context);
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
                        if (isNew && recurring) {
                            entry.autocompleteRecurringDays();
                        }
                        entry = db.save(entry);
                        if (entry_id == null) {
                            entries.add(entry);
                            update(entry.id);

                            if (Utility.languageIs("de", "en")) {
                                Snackbar snackbar = Snackbar.make(scrollView, entry.getRemainingTimeString(context), Snackbar.LENGTH_LONG);
                                snackbar.setBackgroundTint(getResources().getColor(R.color.material_grey));
                                snackbar.show();
                            }
                        } else {
                            update();
                        }
                    }
                    WakeUpReceiver.schedule(context, db);
                },
                hour, min, Utility.is24HourFormat(context));
        // fix broken dialog appearance on some devices
        mTimePicker.setTitle(null);
        mTimePicker.getWindow().setBackgroundDrawableResource(R.drawable.border_dialog);
        mTimePicker.show();
    }

    private void showDatePicker(long timestamp, final Long entry_id) {
        final Context context = this;
        long now = System.currentTimeMillis();
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Math.max(timestamp, now));
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog mDatePicker = new DatePickerDialog(
                context,
                (view, year1, month1, dayOfMonth1) -> {
                    // Bug Android 4.1: Dialog is submitted twice
                    // >> ignore second call to this method.
                    if (!view.isShown()) return;

                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.YEAR, year1);
                    cal.set(Calendar.MONTH, month1);
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    db.updateNextEventAfter(entry_id, cal.getTimeInMillis());
                    for (SimpleTime e : entries) {
                        if (e.id == entry_id) {
                            e.nextEventAfter = cal.getTimeInMillis();
                            break;
                        }
                    }
                    SqliteIntentService.scheduleAlarm(context);
                }, year, month, dayOfMonth);
        mDatePicker.setTitle(R.string.alarmStartDate);
        mDatePicker.getDatePicker().setMinDate(now);
        mDatePicker.show();
    }

    public void onDeleteClick(View view) {
        SimpleTime entry = (SimpleTime) view.getTag();
        db.delete(entry);
        AlarmNotificationService.cancelNotification(this);
        WakeUpReceiver.schedule(this, db);
    }

    public void onNameClicked(View view) {
        SimpleTime entry = (SimpleTime) view.getTag();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.alarmName));
        final EditText input = new EditText(this);
        input.setText(entry.name);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int px = Utility.dpToPx(this, 20);
        params.leftMargin = px;
        params.rightMargin = px;
        input.setLayoutParams(params);
        FrameLayout container = new FrameLayout(this);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            entry.name = input.getText().toString().trim();
            db.save(entry);
            update();
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            //dialog.cancel();
        });

        builder.show();
        //showTimePicker(entry.hour, entry.min, true, entry.id);
    }

    public void onTimeClicked(View view) {
        SimpleTime entry = (SimpleTime) view.getTag();
        showTimePicker(entry.hour, entry.min, true, entry.id);
    }

    public void onDateClicked(View view) {
        SimpleTime entry = (SimpleTime) view.getTag();
        Long nextEventAfter = entry.nextEventAfter;
        long now = System.currentTimeMillis();
        if (nextEventAfter == null || nextEventAfter == 0L || nextEventAfter < now) {
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
            for (int day : days) {
                entry.addRecurringDay(day);
            }
        }
        entry.isActive = true;
        entry = db.save(entry);
        entries.add(entry);
        update(entry.id);
        if (intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false)) {
            new Handler().postDelayed(this::finish, 3000);
        }
    }
}
