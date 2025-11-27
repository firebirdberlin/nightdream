package com.firebirdberlin.nightdream.models;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class SimpleTimeTest {

    private Calendar getReference(int hour, int minute) {
        Calendar reference = Calendar.getInstance();
        reference.set(Calendar.YEAR, 2017);
        reference.set(Calendar.MONTH, Calendar.OCTOBER);
        reference.set(Calendar.DAY_OF_MONTH, 6);
        reference.set(Calendar.HOUR_OF_DAY, hour);
        reference.set(Calendar.MINUTE, minute);
        reference.set(Calendar.SECOND, 33);
        reference.set(Calendar.MILLISECOND, 20);
        return reference;
    }

    private Calendar getCalendarForDate(int year, int month, int dayofMonth) {
        Calendar reference = Calendar.getInstance();
        reference.set(Calendar.YEAR, year);
        reference.set(Calendar.MONTH, month);
        reference.set(Calendar.DAY_OF_MONTH, dayofMonth);
        reference.set(Calendar.HOUR_OF_DAY, 0);
        reference.set(Calendar.MINUTE, 0);
        reference.set(Calendar.SECOND, 0);
        reference.set(Calendar.MILLISECOND, 0);
        return reference;
    }

    @Test
    public void testAddRecurringDay() {
        SimpleTime time = new SimpleTime(13, 25);
        Assert.assertFalse(time.isRecurring());

        time.addRecurringDay(2);

        Assert.assertTrue(time.isRecurring());

        Assert.assertTrue(time.hasDay(2));
        Assert.assertFalse(time.hasDay(1));
        Assert.assertFalse(time.hasDay(3));
        Assert.assertFalse(time.hasDay(4));
        Assert.assertFalse(time.hasDay(5));
        Assert.assertFalse(time.hasDay(6));
        Assert.assertFalse(time.hasDay(7));
    }

    @Test
    public void testRemoveRecurringDay() {
        SimpleTime time = new SimpleTime(13, 25);
        time.addRecurringDay(2);
        time.removeRecurringDay(2);
        Assert.assertFalse(time.isRecurring());
        Assert.assertFalse(time.hasDay(1));
        Assert.assertFalse(time.hasDay(2));
        Assert.assertFalse(time.hasDay(3));
        Assert.assertFalse(time.hasDay(4));
        Assert.assertFalse(time.hasDay(5));
        Assert.assertFalse(time.hasDay(6));
        Assert.assertFalse(time.hasDay(7));
    }

    @Test
    public void testGetNextAlarmTime() {
        SimpleTime time = new SimpleTime(13, 25);
        Calendar reference = getReference(12, 0);
        Calendar next = time.getNextAlarmTime(reference);
        Assert.assertEquals(1507289100000L, next.getTimeInMillis());
    }

    @Test
    public void testGetNextAlarmTime__Midnight() {
        Calendar now = getReference(12, 0);

        SimpleTime time = new SimpleTime(0, 0);
        Calendar next = time.getNextAlarmTime(now);
        Assert.assertEquals(1507327200000L, next.getTimeInMillis());
    }

    @Test
    public void testGetNextAlarmTime_OneOffDelayed__Past() {
        Calendar now = getReference(12, 0);

        SimpleTime time = new SimpleTime(13, 25);
        Calendar startAfter = getCalendarForDate(2017, Calendar.OCTOBER, 4);
        time.nextEventAfter = startAfter.getTimeInMillis();

        Calendar next = time.getNextAlarmTime(now);
        Assert.assertEquals(1507289100000L, next.getTimeInMillis());
    }

    @Test
    public void testGetNextAlarmTime_OneOffDelayed__Future() {
        Calendar now = getReference(12, 0);

        SimpleTime time = new SimpleTime(13, 25);
        Calendar startAfter = getCalendarForDate(2017, Calendar.OCTOBER, 20);
        time.nextEventAfter = startAfter.getTimeInMillis();

        Calendar next = time.getNextAlarmTime(now);
        Assert.assertEquals(1508498700000L, next.getTimeInMillis());
    }


    @Test
    public void testGetNextAlarmTimeTomorrow() {
        SimpleTime time = new SimpleTime(13, 25);
        Calendar reference = getReference(14, 0);
        Calendar next = time.getNextAlarmTime(reference);
        Assert.assertEquals(1507375500000L, next.getTimeInMillis());
    }

    @Test
    public void testGetNextAlarmTimeRecurring() {
        Calendar reference = getReference(12, 0);
        SimpleTime time = new SimpleTime(13, 25, SimpleTime.TUESDAY | SimpleTime.WEDNESDAY);

        Calendar next = time.getNextAlarmTime(reference);
        Assert.assertEquals(1507634700000L, next.getTimeInMillis());
    }

    @Test
    public void testGetNextAlarmTimeRecurring__TuesdayIsMuted() {
        Calendar reference = getReference(12, 0);
        SimpleTime time = new SimpleTime(13, 25, SimpleTime.TUESDAY | SimpleTime.WEDNESDAY);
        time.nextEventAfter = 1507634700000L;

        Calendar next = time.getNextAlarmTime(reference);
        Assert.assertEquals(1507721100000L, next.getTimeInMillis());
    }

    @Test
    public void testGetNextAlarmTimeRecurring__WednesdayIsMutedAsWell() {
        Calendar reference = getReference(12, 0);
        SimpleTime time = new SimpleTime(13, 25, SimpleTime.TUESDAY | SimpleTime.WEDNESDAY);
        time.nextEventAfter = 1507721100000L;

        Calendar next = time.getNextAlarmTime(reference);
        Assert.assertEquals(1508239500000L, next.getTimeInMillis());
    }

    @Test
    public void testGetNextAlarmTimeRecurring2() {
        Calendar reference = getReference(12, 0);
        SimpleTime time = new SimpleTime(13, 25, SimpleTime.TUESDAY | SimpleTime.SATURDAY);

        Calendar next = time.getNextAlarmTime(reference);
        Assert.assertEquals(1507375500000L, next.getTimeInMillis());
    }

    @Test
    public void testGetNextAlarmTimeRecurringToday() {
        Calendar reference = getReference(12, 0);
        SimpleTime time = new SimpleTime(13, 25, SimpleTime.FRIDAY | SimpleTime.SATURDAY);

        Calendar next = time.getNextAlarmTime(reference);
        Assert.assertEquals(1507289100000L, next.getTimeInMillis());
    }

    @Test
    public void testGetNextAlarmTimeRecurringSameWeekDay() {
        Calendar reference = getReference(14, 0);
        SimpleTime time = new SimpleTime(13, 25, SimpleTime.FRIDAY);

        Calendar next = time.getNextAlarmTime(reference);
        Assert.assertEquals(1507893900000L, next.getTimeInMillis());
    }

    @Test
    public void testGetNextFromList() {
        List<SimpleTime> times = Arrays.asList(
                new SimpleTime(10, 20, SimpleTime.TUESDAY | SimpleTime.FRIDAY),
                new SimpleTime(13, 25, SimpleTime.MONDAY | SimpleTime.WEDNESDAY)
        );

        times.get(0).isActive = true;
        times.get(1).isActive = true;

        Calendar reference = getReference(14, 0);
        SimpleTime result = SimpleTime.getNextFromList(times, reference);
        Assert.assertEquals(times.get(1), result);

        Calendar reference2 = getReference(10, 0);
        SimpleTime result2 = SimpleTime.getNextFromList(times, reference2);
        Assert.assertEquals(times.get(0), result2);
    }

    @Test
    public void testGetNextFromListReturnNull() {
        List<SimpleTime> times = Arrays.asList();
        Calendar reference = getReference(14, 0);
        SimpleTime result = SimpleTime.getNextFromList(times, reference);
        Assert.assertEquals(null, result);
    }

}
