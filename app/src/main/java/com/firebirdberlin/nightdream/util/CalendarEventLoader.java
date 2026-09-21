package com.firebirdberlin.nightdream.util;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
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


    public static class CalendarEvents {
        public final HashSet<LocalDate> oneTimeEvents;
        public final HashSet<LocalDate> recurringEvents;

        CalendarEvents(HashSet<LocalDate> oneTimeEvents, HashSet<LocalDate> recurringEvents) {
            this.oneTimeEvents = oneTimeEvents;
            this.recurringEvents = recurringEvents;
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

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_CALENDAR permission not granted. Cannot load events.");
            return new CalendarEvents(new HashSet<>(), new HashSet<>());
        }

        Log.d(TAG, "Cache expired or permission granted. Fetching real calendar events.");

        HashSet<LocalDate> eventDays = new HashSet<>();
        HashSet<LocalDate> recurringEventDays = new HashSet<>();

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
                CalendarContract.Events.RRULE              // 2: The recurrence rule
        };

        // 3. Query the Instances table for the specified time range.
        Cursor cursor = CalendarContract.Instances.query(cr, INSTANCE_PROJECTION, startMillis, endMillis);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    long beginVal = cursor.getLong(0);
                    String rrule = cursor.getString(2);

                    boolean isRecurring = (rrule != null && !rrule.isEmpty());

                    LocalDate day = Instant.ofEpochMilli(beginVal)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    if (isRecurring) {
                        recurringEventDays.add(day);
                    } else {
                        eventDays.add(day);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        cachedEvents = new CalendarEvents(eventDays, recurringEventDays);
        lastCacheTimeMillis = currentTimeMillis;
        return cachedEvents;
    }
}
