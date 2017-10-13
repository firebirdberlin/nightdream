package com.firebirdberlin.nightdream.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class SimpleTime {
    public static int SUNDAY = 1;
    public static int MONDAY = 1 << 1;
    public static int TUESDAY = 1 << 2;
    public static int WEDNESDAY = 1 << 3;
    public static int THURSDAY = 1 << 4;
    public static int FRIDAY = 1 << 5;
    public static int SATURDAY = 1 << 6;

    public long id = -1L;
    public int hour = 0;
    public int min = 0;
    public int recurringDays = 0;

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

    public static long getNextAlarmTime(List<SimpleTime> entries, Calendar reference) {
        List<Long> times = new ArrayList<>();

        for (SimpleTime t : entries) {
            try {
                times.add(t.getNextAlarmTime(reference).getTimeInMillis());
            } catch (NullPointerException e) {

            }
        }

        if (times.size() > 0) {
            Collections.sort(times);
            return times.get(0);
        }
        return -1L;
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

    private Calendar initCalendar(Calendar reference) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(reference.getTimeInMillis());
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    public void addRecurringDays(int flags) {
        recurringDays = flags;
    }

    private boolean isRecurring() {
        return recurringDays != 0;
    }

    private Calendar getNextRecurringAlarmTime(Calendar reference) {
        List<Long> times = new ArrayList<>();
        List<Integer> days = Arrays.asList(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
                Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY);

        for (Integer day : days) {
            // map the day to the corresponding bit flag
            int flag = 1 << (day - 1);
            Calendar cal = initCalendar(reference);

            if ((recurringDays & flag) == flag) {

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
}

