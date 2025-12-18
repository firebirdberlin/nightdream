package com.firebirdberlin.nightdream.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.prolificinteractive.materialcalendarview.CalendarDay;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * A utility class to load calendar events.
 */
public class CalendarEventLoader {

    private static final String TAG = "CalendarEventLoader";
    private static final long CACHE_DURATION_MS = TimeUnit.MINUTES.toMillis(30);

    private static CalendarEvents cachedEvents;
    private static long lastCacheTimeMillis = 0;
    private static String alarmTitle = "alarm";


    public static class CalendarEvents {
        public final HashSet<CalendarDay> oneTimeEvents;
        public final HashSet<CalendarDay> recurringEvents;
        public final HashSet<Long> upcomingAlarmTimes;

        CalendarEvents(HashSet<CalendarDay> oneTimeEvents, HashSet<CalendarDay> recurringEvents, HashSet<Long> upcomingAlarmTimes) {
            this.oneTimeEvents = oneTimeEvents;
            this.recurringEvents = recurringEvents;
            this.upcomingAlarmTimes = upcomingAlarmTimes;
        }
    }

    /**
     * Sets the keyword used to identify alarm events.
     * @param title The keyword (e.g., "alarm").
     */
    public static void setAlarmTitle(String title) {
        if (title != null && !title.isEmpty()) {
            alarmTitle = title.toLowerCase();
        }
    }

    /**
     * Loads one-time and recurring calendar events within a specified time range, using a cache.
     * The cache is invalidated every 30 minutes.
     *
     * @param context The context.
     * @return A CalendarEvents object containing sets of days for one-time and recurring events.
     */
    public static synchronized CalendarEvents loadEvents(Context context) {
        long currentTimeMillis = System.currentTimeMillis();
        if (cachedEvents != null && (currentTimeMillis - lastCacheTimeMillis) < CACHE_DURATION_MS) {
            Log.d(TAG, "Returning cached calendar events.");
            return cachedEvents;
        }

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_CALENDAR permission not granted. Cannot load events.");
            return new CalendarEvents(new HashSet<>(), new HashSet<>(), new HashSet<>());
        }

        Log.d(TAG, "Cache expired or permission granted. Fetching real calendar events.");

        HashSet<CalendarDay> eventDays = new HashSet<>();
        HashSet<CalendarDay> recurringEventDays = new HashSet<>();
        HashSet<Long> alarmEventTimes = new HashSet<>();

        // 1. Define the time range for which you want to load events.
        // Let's load events for the next 365 days and the past year.
        Calendar beginTime = Calendar.getInstance();
        beginTime.add(Calendar.MONTH, -3);
        long startMillis = beginTime.getTimeInMillis();

        Calendar endTime = Calendar.getInstance();
        endTime.add(Calendar.YEAR, 1);
        long endMillis = endTime.getTimeInMillis();

        ContentResolver cr = context.getContentResolver();

        // 2. Define the columns you need from the Instances table.
        final String[] INSTANCE_PROJECTION = new String[]{
                CalendarContract.Instances.BEGIN,          // 0: The start time of the instance
                CalendarContract.Instances.EVENT_ID,       // 1: The original event's ID
                CalendarContract.Instances.TITLE           // 2: The title
        };

        // 3. Query the Instances table for the specified time range.
        Cursor cursor = CalendarContract.Instances.query(cr, INSTANCE_PROJECTION, startMillis, endMillis);

        // Use a map to cache the recurrence status of each event ID to avoid redundant queries.
        HashMap<Long, Boolean> recurrenceStatusCache = new HashMap<>();

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    long beginVal = cursor.getLong(0);
                    long eventId = cursor.getLong(1);
                    String title = cursor.getString(2);

                    boolean isRecurring;

                    // Check cache first
                    if (recurrenceStatusCache.containsKey(eventId)) {
                        isRecurring = recurrenceStatusCache.get(eventId);
                    } else {
                        // If not in cache, query the Events table for this specific event's RRULE.
                        Uri eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
                        try (Cursor eventCursor = cr.query(eventUri, new String[]{CalendarContract.Events.RRULE}, null, null, null)) {
                            isRecurring = false;
                            if (eventCursor != null && eventCursor.moveToFirst()) {
                                String rrule = eventCursor.getString(0);
                                isRecurring = (rrule != null && !rrule.isEmpty());
                            }
                        }
                        recurrenceStatusCache.put(eventId, isRecurring);
                    }

                    boolean isAlarm = title != null && title.toLowerCase().startsWith(alarmTitle);

                    if (isAlarm) {
                        if (beginVal >= currentTimeMillis) {
                            alarmEventTimes.add(beginVal);
                        }
                    } else {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(beginVal);
                        CalendarDay day = CalendarDay.from(calendar);

                        if (isRecurring) {
                            recurringEventDays.add(day);
                        } else {
                            eventDays.add(day);
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }
        cachedEvents = new CalendarEvents(eventDays, recurringEventDays, alarmEventTimes);
        lastCacheTimeMillis = currentTimeMillis;
        return cachedEvents;
    }

    /**
     * Returns the next upcoming alarm time in milliseconds, or 0 if no upcoming alarms are found.
     * This method is designed to be lightweight and does not use the cache.
     *
     * @param context The context.
     * @return The next alarm time in milliseconds.
     */
    public static long loadNextAlarmTime(Context context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            return 0;
        }

        long currentTimeMillis = System.currentTimeMillis();
        Calendar endTime = Calendar.getInstance();
        endTime.add(Calendar.YEAR, 1);
        long endMillis = endTime.getTimeInMillis();

        ContentResolver cr = context.getContentResolver();
        final String[] INSTANCE_PROJECTION = new String[]{
                CalendarContract.Instances.BEGIN,          // 0: The start time of the instance
                CalendarContract.Instances.TITLE           // 1: The title
        };

        Uri uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(currentTimeMillis))
                .appendPath(String.valueOf(endMillis))
                .build();

        String sortOrder = CalendarContract.Instances.BEGIN + " ASC";

        try (Cursor cursor = cr.query(uri, INSTANCE_PROJECTION, null, null, sortOrder)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String title = cursor.getString(1);
                    if (title != null && title.toLowerCase().startsWith(alarmTitle)) {
                        return cursor.getLong(0);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading next alarm time", e);
        }

        return 0;
    }

    /**
     * Invalidates the cache, forcing a reload of events on the next call to loadEvents.
     */
    public static synchronized void invalidateCache() {
        cachedEvents = null;
        lastCacheTimeMillis = 0;
        Log.d(TAG, "Calendar events cache invalidated.");
    }
}
