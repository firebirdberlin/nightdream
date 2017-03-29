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
        this.start = start;
        this.end = end;
        this.start = fixDate(this.start);
        this.end = fixDate(this.end);
    }

    private Calendar fixDate(Calendar cal) {
        Calendar now = Calendar.getInstance();
        // we're only interested in the time, the date shall be today.
        cal.set(Calendar.YEAR, now.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, now.get(Calendar.MONTH));
        cal.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

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
        return fixDate(cal);
    }

    public Calendar getNextEvent() {
        if (start.before(end)) {
            return start;
        }
        return end;
    }
}

