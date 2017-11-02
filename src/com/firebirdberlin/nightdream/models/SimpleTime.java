package com.firebirdberlin.nightdream.models;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

public class SimpleTime {
    public static int SUNDAY = 1;
    public static int MONDAY = 1 << 1;
    public static int TUESDAY = 1 << 2;
    public static int WEDNESDAY = 1 << 3;
    public static int THURSDAY = 1 << 4;
    public static int FRIDAY = 1 << 5;
    public static int SATURDAY = 1 << 6;
    public static List<Integer> DAYS = Arrays.asList(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
            Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY);

    public long id = -1L;
    public int hour = 0;
    public int min = 0;
    public int recurringDays = 0;
    public boolean isActive = false;
    public boolean isNextAlarm = false;

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
    }

    public static SimpleTime getNextFromList(List<SimpleTime> entries) {
        Calendar now = Calendar.getInstance();
        return getNextFromList(entries, now);
    }

    protected static SimpleTime getNextFromList(List<SimpleTime> entries, Calendar reference) {
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

    public Calendar getNextAlarmTime(Calendar reference) {
        return getCalendar(reference);
    }

    public Calendar getCalendar(Calendar reference) {
        if (!isRecurring()) {
            Calendar cal = initCalendar(reference);
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
        return bundle;
    }

    private Calendar initCalendar(Calendar reference) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(reference.getTimeInMillis());
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
        for (Integer day : SimpleTime.DAYS) {
            Calendar cal = initCalendar(reference);
            if (hasDay(day)) {
                cal.set(Calendar.DAY_OF_WEEK, day);
                if (cal.before(reference)) {
                    cal.add(Calendar.DATE, 7);
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
}

