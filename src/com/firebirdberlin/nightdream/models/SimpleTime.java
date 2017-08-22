package com.firebirdberlin.nightdream.models;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class SimpleTime {

    public int hour = 0;
    public int min = 0;

    public SimpleTime() {

    }

    public SimpleTime(long millis) {
        if (millis < 0L) return;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        min = calendar.get(Calendar.MINUTE);
    }

    public SimpleTime(int hour, int min){
        this.hour = hour;
        this.min = min;
    }

    public SimpleTime(int minutes){
        this.hour = minutes / 60;
        this.min = minutes % 60;
    }

    public int toMinutes() {
        return this.hour * 60 + this.min;
    }

    public long getMillis() {
        return getCalendar().getTimeInMillis();
    }

    public Calendar getCalendar() {
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Calendar now = Calendar.getInstance();
        if ( cal.before(now) ) {
            cal.add(Calendar.DATE, 1);
        }

        return cal;
    }
}

