package com.firebirdberlin.nightdream;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class SimpleTime {

    public int hour = 0;
    public int min = 0;

    SimpleTime() {

    }

    SimpleTime(long millis){
        int minutes = (int) (millis / 60000);
        hour = minutes / 60;
        min = minutes % 60;
    }

    SimpleTime(int hour, int min){
        this.hour = hour;
        this.min = min;
    }

    long getMillis(){
        return  hour * 60 * 60 * 1000L + min * 60 * 1000L;
    }

    public Calendar getCalendar() {
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }
}

