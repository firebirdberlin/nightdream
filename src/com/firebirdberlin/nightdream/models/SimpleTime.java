package com.firebirdberlin.nightdream.models;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;

import com.firebirdberlin.nightdream.R;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class SimpleTime {
    public static int SUNDAY = 1;
    public static int MONDAY = 1 << 1;
    public static int TUESDAY = 1 << 2;
    public static int WEDNESDAY = 1 << 3;
    public static int THURSDAY = 1 << 4;
    public static int FRIDAY = 1 << 5;
    public static int SATURDAY = 1 << 6;
    public static List<Integer> DAYS = Arrays.asList(
            Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    );

    public long id = -1L;
    public int hour = 0;
    public int min = 0;
    public int recurringDays = 0;
    public boolean isActive = false;
    public boolean isNextAlarm = false;
    public int radioStationIndex = -1;
    public String soundUri;
    public Long nextEventAfter = null;
    public boolean vibrate = false;
    public int numAutoSnoozeCycles = 0;

    public SimpleTime() {

    }

    public SimpleTime(long millis) {
        if (millis < 0L) return;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        min = calendar.get(Calendar.MINUTE);
    }

    public SimpleTime(int hour, int min) {
        this.hour = hour;
        this.min = min;
    }

    public SimpleTime(int hour, int min, int recurringDays) {
        this.hour = hour;
        this.min = min;
        this.recurringDays = recurringDays;
    }

    public SimpleTime(long id, int hour, int min, int recurringDays) {
        this.id = id;
        this.hour = hour;
        this.min = min;
        this.recurringDays = recurringDays;
    }

    public SimpleTime(int minutes) {
        this.hour = minutes / 60;
        this.min = minutes % 60;
    }

    public SimpleTime(Bundle bundle) {
        this.id = bundle.getLong("id", -1L);
        this.hour = bundle.getInt("hour", 0);
        this.min = bundle.getInt("min", 0);
        this.recurringDays = bundle.getInt("recurringDays", 0);
        this.isActive = bundle.getBoolean("isActive", false);
        this.isNextAlarm = bundle.getBoolean("isNextAlarm", false);
        this.soundUri = bundle.getString("soundUri");
        this.nextEventAfter = bundle.getLong("nextEventAfter");
        this.radioStationIndex = bundle.getInt("radioStationIndex", -1);
        this.vibrate = bundle.getBoolean("vibrate", false);
        this.numAutoSnoozeCycles = bundle.getInt("numAutoSnoozeCycles", 0);
    }

    public static SimpleTime getNextFromList(List<SimpleTime> entries) {
        Calendar now = Calendar.getInstance();
        return getNextFromList(entries, now);
    }

    static SimpleTime getNextFromList(List<SimpleTime> entries, Calendar reference) {
        TreeMap<Calendar, SimpleTime> map = new TreeMap<>();
        for (SimpleTime t : entries) {
            Calendar time = t.getNextAlarmTime(reference);
            if (t.isActive && time != null) {
                map.put(time, t);
            }
        }

        if (map.size() > 0) {
            return map.firstEntry().getValue();
        }
        return null;
    }

    public int toMinutes() {
        return this.hour * 60 + this.min;
    }

    public long getMillis() {
        return getCalendar().getTimeInMillis();
    }

    public Calendar getCalendar() {
        Calendar now = Calendar.getInstance();
        return getCalendar(now);
    }

    public Calendar getTodaysAlarmTIme() {
        Calendar now = Calendar.getInstance();
        Calendar todaysAlarmTime = initCalendar(now);
        if (!isRecurring()) {
            return todaysAlarmTime;
        } else {
            int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
            if ( hasDay(dayOfWeek) ) {
                return todaysAlarmTime;
            }
        }
        return null;
    }

    public Calendar getNextAlarmTime(Calendar reference) {
        return getCalendar(reference);
    }

    public Calendar getCalendar(Calendar reference) {
        if (!isRecurring()) {
            Calendar cal = initCalendar(reference);
            if (nextEventAfter != null) {
                Calendar next = initCalendar(nextEventAfter);
                if (next.after(cal)) {
                    cal = next;
                }
            }
            if (cal.before(reference)) {
                cal.add(Calendar.DATE, 1);
            }
            return cal;
        } else {
            return getNextRecurringAlarmTime(reference);
        }
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putLong("id", this.id);
        bundle.putInt("hour", this.hour);
        bundle.putInt("min", this.min);
        bundle.putInt("recurringDays", this.recurringDays);
        bundle.putBoolean("isNextAlarm", this.isNextAlarm);
        bundle.putBoolean("isActive", this.isActive);
        bundle.putInt("alarmTimeMinutes", this.toMinutes());
        bundle.putString("soundUri", this.soundUri);
        bundle.putInt("radioStationIndex", this.radioStationIndex);
        bundle.putBoolean("vibrate", this.vibrate);
        bundle.putInt("numAutoSnoozeCycles", this.numAutoSnoozeCycles);
        if (this.nextEventAfter != null) {
            bundle.putLong("nextEventAfter", this.nextEventAfter);
        }
        return bundle;
    }

    private Calendar initCalendar(Calendar reference) {
        return initCalendar(reference.getTimeInMillis());
    }

    private Calendar initCalendar(long reference) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(reference);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    public void addRecurringDay(int day) {
        recurringDays |= dayConstantToFlag(day);
    }

    public void removeRecurringDay(int day) {
        recurringDays &= ~dayConstantToFlag(day);
    }

    public boolean isRecurring() {
        return recurringDays != 0;
    }

    private Calendar getNextRecurringAlarmTime(Calendar reference) {
        List<Long> times = new ArrayList<>();
        Calendar firstEventTime = null;
        if (nextEventAfter != null) {
            firstEventTime = Calendar.getInstance();
            firstEventTime.setTimeInMillis(nextEventAfter);
        }
        for (Integer day : SimpleTime.DAYS) {
            if (hasDay(day)) {
                Calendar cal = null;
                if (firstEventTime != null) {
                    cal = initCalendar(firstEventTime);
                    cal.set(Calendar.DAY_OF_WEEK, day);
                    if (!cal.after(firstEventTime)) {
                        cal.add(Calendar.DATE, 7);
                    }

                    if (!cal.after(reference)) {
                        cal = null;
                    }
                }

                if (cal == null) {
                    cal = initCalendar(reference);
                    cal.set(Calendar.DAY_OF_WEEK, day);
                    // Usually the reference date refers to 'now'. Events shall not be raised in
                    // the past
                    if (cal.before(reference)) {
                        cal.add(Calendar.DATE, 7);
                    }
                }
                times.add(cal.getTimeInMillis());
            }
        }

        Collections.sort(times);

        Calendar result = Calendar.getInstance();
        result.setTimeInMillis(times.get(0));
        return result;
    }

    public boolean hasDay(int day) {
        // map the day to the corresponding bit flag
        int flag = 1 << (day - 1);
        return ((recurringDays & flag) == flag);
    }

    public int dayConstantToFlag(int day) {
        return (1 << (day - 1));
    }

    @Override
    public String toString() {
        return String.format("%02d:%02d", hour, min);
    }

    public String getWeekDaysAsString() {
        DateFormatSymbols symbols = new DateFormatSymbols();
        String[] dayNames = symbols.getShortWeekdays();
        int firstDayOfWeek = Calendar.getInstance().getFirstDayOfWeek();
        StringBuilder builder = new StringBuilder();
        for (int d = 0; d < 7; d++) {
            int day = (firstDayOfWeek - 1 + d) % 7 + 1;
            if (hasDay(day)) {
                if (!builder.toString().isEmpty()) builder.append(", ");
                builder.append(dayNames[day]);
            }
        }

        return builder.toString();
    }

    public void autocompleteRecurringDays() {
        Calendar nextAlarm = getCalendar();
        int dayOfWeek = nextAlarm.get(Calendar.DAY_OF_WEEK);
        if (Calendar.SATURDAY == dayOfWeek || Calendar.SUNDAY == dayOfWeek) {
            addRecurringDay(Calendar.SATURDAY);
            addRecurringDay(Calendar.SUNDAY);
        } else {
            addRecurringDay(Calendar.MONDAY);
            addRecurringDay(Calendar.TUESDAY);
            addRecurringDay(Calendar.WEDNESDAY);
            addRecurringDay(Calendar.THURSDAY);
            addRecurringDay(Calendar.FRIDAY);
        }
    }

    public long getRemainingMillis() {
        return getMillis() - System.currentTimeMillis();
    }

    public String getRemainingTimeString(Context context) {
        String returnString;
        long diffMinutes = TimeUnit.MILLISECONDS.toMinutes(getRemainingMillis());
        int days = (int) TimeUnit.MINUTES.toDays(diffMinutes);
        int hours = (int) (TimeUnit.MINUTES.toHours(diffMinutes) % TimeUnit.DAYS.toHours(1));
        int minutes = (int) (diffMinutes % TimeUnit.HOURS.toMinutes(1));

        Resources res = context.getResources();
        if (days == 0 && hours == 0 && minutes == 0) {
            returnString = res.getString(R.string.remaining_alarm_time_now);
        } else {
            returnString = res.getString(R.string.remaining_alarm_time_start);

            if (days > 0) {
                returnString += res.getQuantityString(R.plurals.duration_days_relative_future, days, days);
            }

            if (hours > 0) {
                returnString += res.getQuantityString(R.plurals.duration_hours_relative_future, hours, hours);
            }

            if (minutes > 0) {
                returnString += res.getQuantityString(R.plurals.duration_minutes_relative_future, minutes, minutes);
            }

            returnString += res.getString(R.string.remaining_alarm_time_end);
        }

        return returnString;
    }
}

