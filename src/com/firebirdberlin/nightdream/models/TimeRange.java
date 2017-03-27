package com.firebirdberlin.nightdream.models;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class TimeRange {

    public Calendar start;
    public Calendar end;


    public TimeRange() {
        this.start = Calendar.getInstance();
        this.end = Calendar.getInstance();
    }

    public TimeRange(long start, long end){
        this.start = getCalendar(start);
        this.end = getCalendar(end);
    }

    public TimeRange(Calendar start, Calendar end){
        this.start = fixDate(start);
        this.end = fixDate(end);
    }

    private Calendar fixDate(Calendar cal) {
        Calendar now = Calendar.getInstance();
        if ( cal.before(now) ) {
            cal.add(Calendar.DATE, 1);
        }
        return cal;
    }


    public boolean inRange() {
        Calendar now = Calendar.getInstance();
        return inRange(now);
    }

    public boolean inRange(Calendar time) {
        if (end.before(start)){
            return ( time.after(start) || time.before(end) );
        } else if (! start.equals(end)) {
            return ( time.after(start) && time.before(end) );
        }
        return true;
    }

    public Calendar getCalendar(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        Calendar now = Calendar.getInstance();
        if ( cal.before(now) ) {
            cal.add(Calendar.DATE, 1);
        }
        return cal;
    }

    public Calendar getNextEvent() {
        if (start.before(end)) {
            return start;
        }
        return end;
    }
}

