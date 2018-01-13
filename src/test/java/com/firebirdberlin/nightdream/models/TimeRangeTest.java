package com.firebirdberlin.nightdream.models;

import junit.framework.TestCase;

import java.util.Calendar;

public class TimeRangeTest extends TestCase {
    private Calendar getCalendar(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

//        Calendar now = Calendar.getInstance();
//        if ( cal.before(now) ) {
//            cal.add(Calendar.DATE, 1);
//        }

        return cal;
    }

    public void testInRange() throws Exception {
        Calendar start = getCalendar(10, 20);
        Calendar end = getCalendar(12, 20);
        TimeRange range = new TimeRange(start, end);

        assertFalse(range.inRange(getCalendar(10, 19)));
        assertTrue(range.inRange(getCalendar(10, 20)));
        assertTrue(range.inRange(getCalendar(10, 48)));
        assertTrue(range.inRange(getCalendar(12, 19)));
        assertFalse(range.inRange(getCalendar(12, 20)));
        assertFalse(range.inRange(getCalendar(12, 21)));
    }

    public void testInRange1() throws Exception {
        Calendar start = getCalendar(0, 0);
        Calendar end = getCalendar(0, 0);

        // always in range
        TimeRange range = new TimeRange(start, end);
        assertTrue(range.inRange(getCalendar(0, 0)));
        assertTrue(range.inRange(getCalendar(12, 0)));
        assertTrue(range.inRange(getCalendar(15, 23)));
    }

    public void testInRange2() throws Exception {
        Calendar now = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        start.add(Calendar.DATE, -1);
        start.add(Calendar.HOUR_OF_DAY, -2);
        end.add(Calendar.DATE, -2);
        end.add(Calendar.HOUR_OF_DAY, -1);

        // always in range
        TimeRange range = new TimeRange(start, end);
        assertFalse(range.inRange(now));
    }

    public void testInRange3() throws Exception {
        Calendar now = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        start.add(Calendar.DATE, -1);
        start.add(Calendar.HOUR_OF_DAY, +1);
        end.add(Calendar.DATE, -2);
        end.add(Calendar.HOUR_OF_DAY, +2);

        // always in range
        TimeRange range = new TimeRange(start, end);
        assertFalse(range.inRange(now));
    }

    public void testInRange4() throws Exception {
        Calendar now = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        start.add(Calendar.DATE, -1);
        start.add(Calendar.HOUR_OF_DAY, -1);
        end.add(Calendar.DATE, -2);
        end.add(Calendar.HOUR_OF_DAY, +1);

        // always in range
        TimeRange range = new TimeRange(start, end);
        assertTrue(range.inRange(now));
    }


    public void testGetNextEvent__now__inRange() throws Exception {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        start.add(Calendar.HOUR_OF_DAY, -1);
        end.add(Calendar.HOUR_OF_DAY, +1);
        Calendar expected = (Calendar) end.clone();

        start.add(Calendar.DATE, -10);
        end.add(Calendar.DATE, -5);

        TimeRange range = new TimeRange(start, end);
        assertEquals(expected, range.getNextEvent());
    }

    public void testGetNextEvent__start__today() throws Exception {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        start.add(Calendar.HOUR_OF_DAY, +1);
        Calendar expected = (Calendar) start.clone();

        end.add(Calendar.HOUR_OF_DAY, +2);
        start.add(Calendar.DATE, -10);
        end.add(Calendar.DATE, -5);

        TimeRange range = new TimeRange(start, end);
        assertEquals(expected, range.getNextEvent());
    }

    public void testGetNextEvent__start__tomorrow() throws Exception {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        start.add(Calendar.HOUR_OF_DAY, -2);
        end.add(Calendar.HOUR_OF_DAY, -1);

        TimeRange range = new TimeRange(start, end);
        Calendar expected = sameTimeTommorrow(start);

        assertEquals(expected, range.getNextEvent());
    }

    public void testGetNextEvent__end__tomorrow() throws Exception {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        end.add(Calendar.HOUR_OF_DAY, +23);

        TimeRange range = new TimeRange(start, end);
        assertEquals(end, range.getNextEvent());
    }

    Calendar sameTimeTommorrow(Calendar from) {
        Calendar cal = (Calendar) from.clone();
        cal.setTimeInMillis(from.getTimeInMillis());
        cal.add(Calendar.DATE, 1);
        return cal;
    }
}