package com.firebirdberlin.nightdream;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.firebirdberlin.nightdream.events.OnAlarmEntryChanged;
import com.firebirdberlin.nightdream.events.OnAlarmEntryDeleted;
import com.firebirdberlin.nightdream.models.SimpleTime;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public class DataSource {

    private SQLiteDBHelper mDBHelper;
    private SQLiteDatabase db = null;

    public DataSource(Context context) {
        mDBHelper = new SQLiteDBHelper(context);
    }

    public void open() throws SQLException {
        if (db == null) {
            db = mDBHelper.getWritableDatabase();
        }
    }

    public void close() {
        mDBHelper.close();
    }

    public SimpleTime save(SimpleTime time) {
        return save(time, true);
    }

    public SimpleTime save(SimpleTime time, boolean raise_event) {
        if (time.id == -1L) {
            return insert(time);
        } else {
            return update(time, raise_event);
        }
    }

    private SimpleTime insert(SimpleTime time) {
        ContentValues values = toContentValues(time);
        if (db == null) return null;
        long new_id = db.insert(SQLiteDBHelper.AlarmEntry.TABLE_NAME, null, values);
        time.id = new_id;
        return time;
    }

    private SimpleTime update(SimpleTime time, boolean raise_event) {
        ContentValues values = toContentValues(time);

        String selection = SQLiteDBHelper.AlarmEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(time.id)};

        db.update(SQLiteDBHelper.AlarmEntry.TABLE_NAME, values, selection, selectionArgs);
        if (raise_event) {
            EventBus.getDefault().post(new OnAlarmEntryChanged(time));
        }
        return time;
    }

    private ContentValues toContentValues(SimpleTime time) {
        ContentValues values = new ContentValues();
        if (time.id != -1L) {
            values.put(SQLiteDBHelper.AlarmEntry._ID, time.id);
        }
        values.put(SQLiteDBHelper.AlarmEntry.COLUMN_HOUR, time.hour);
        values.put(SQLiteDBHelper.AlarmEntry.COLUMN_MINUTE, time.min);
        values.put(SQLiteDBHelper.AlarmEntry.COLUMN_DAYS, time.recurringDays);
        values.put(SQLiteDBHelper.AlarmEntry.COLUMN_IS_ACTIVE, time.isActive);
        values.put(SQLiteDBHelper.AlarmEntry.COLUMN_IS_NEXT_ALARM, time.isNextAlarm);
        values.put(SQLiteDBHelper.AlarmEntry.COLUMN_ALARM_SOUND_URI, time.soundUri);
        values.put(SQLiteDBHelper.AlarmEntry.COLUMN_NEXT_EVENT_AFTER, time.nextEventAfter);
        return values;
    }

    public void delete(SimpleTime time) {
        String selection = SQLiteDBHelper.AlarmEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(time.id)};

        db.delete(SQLiteDBHelper.AlarmEntry.TABLE_NAME, selection, selectionArgs);
        EventBus.getDefault().post(new OnAlarmEntryDeleted(time));
    }

    public void updateNextEventAfter(long alarmTimeId, long nextEventAfter) {
        ContentValues values = new ContentValues();
        values.put(SQLiteDBHelper.AlarmEntry.COLUMN_NEXT_EVENT_AFTER, nextEventAfter);

        String selection = SQLiteDBHelper.AlarmEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(alarmTimeId)};

        db.update(SQLiteDBHelper.AlarmEntry.TABLE_NAME, values, selection, selectionArgs);
        SimpleTime time = getAlarmEntry(alarmTimeId);
        if (time != null) {
            EventBus.getDefault().post(new OnAlarmEntryChanged(time));
        }
    }

    public void cancelPendingAlarms() {
        String selection = SQLiteDBHelper.AlarmEntry.COLUMN_IS_NEXT_ALARM + " = ? AND " +
                SQLiteDBHelper.AlarmEntry.COLUMN_DAYS + " = ? AND " +
                SQLiteDBHelper.AlarmEntry.COLUMN_IS_ACTIVE + " = ?";
        String[] selectionArgs = {"1", "0", "1"};

        db.delete(SQLiteDBHelper.AlarmEntry.TABLE_NAME, selection, selectionArgs);
    }


    public SimpleTime getNextAlarmToSchedule() {
        List<SimpleTime> entries = getAlarms();
        return SimpleTime.getNextFromList(entries);
    }

    public List<SimpleTime> getAlarms() {
        Cursor cursor = getQueryCursor(null, null);
        List<SimpleTime> items = new ArrayList<>();
        while (cursor.moveToNext()) {
            SimpleTime time = cursorToSimpleTime(cursor);
            items.add(time);
        }
        cursor.close();
        return items;
    }

    public SimpleTime getNextAlarmEntry() {
        String where = SQLiteDBHelper.AlarmEntry.COLUMN_IS_NEXT_ALARM + " = ? AND "
                + SQLiteDBHelper.AlarmEntry.COLUMN_IS_ACTIVE  + " = ?";
        String[] whereArgs = {"1", "1"};
        Cursor cursor = getQueryCursor(where, whereArgs);
        if ( cursor.moveToFirst() ) {
            return cursorToSimpleTime(cursor);
        }
        return null;
    }

    public SimpleTime getAlarmEntry(long id) {
        String where = SQLiteDBHelper.AlarmEntry._ID + " = ? ";
        String[] whereArgs = {String.valueOf(id)};
        Cursor cursor = getQueryCursor(where, whereArgs);
        if (cursor.moveToFirst()) {
            return cursorToSimpleTime(cursor);
        }
        return null;
    }

    private Cursor getQueryCursor(String where, String[] whereArgs) {
        String[] projection = {
                SQLiteDBHelper.AlarmEntry._ID,
                SQLiteDBHelper.AlarmEntry.COLUMN_HOUR,
                SQLiteDBHelper.AlarmEntry.COLUMN_MINUTE,
                SQLiteDBHelper.AlarmEntry.COLUMN_DAYS,
                SQLiteDBHelper.AlarmEntry.COLUMN_IS_ACTIVE,
                SQLiteDBHelper.AlarmEntry.COLUMN_IS_NEXT_ALARM,
                SQLiteDBHelper.AlarmEntry.COLUMN_ALARM_SOUND_URI,
                SQLiteDBHelper.AlarmEntry.COLUMN_NEXT_EVENT_AFTER
        };

        return db.query(SQLiteDBHelper.AlarmEntry.TABLE_NAME, projection, where, whereArgs,
                        null, null, null);
    }

    private SimpleTime cursorToSimpleTime(Cursor cursor) {
        long id = cursor.getLong(0);
        int hour = cursor.getInt(1);
        int minute = cursor.getInt(2);
        int days = cursor.getInt(3);

        SimpleTime time = new SimpleTime(id, hour, minute, days);
        time.isActive = (cursor.getInt(4) == 1);
        time.isNextAlarm = (cursor.getInt(5) == 1);
        time.soundUri = cursor.getString(6);
        time.nextEventAfter = cursor.getLong(7);
        return time;
    }

    public SimpleTime setNextAlarm(SimpleTime entry) {
        db.execSQL("UPDATE " + SQLiteDBHelper.AlarmEntry.TABLE_NAME +
                   " SET " + SQLiteDBHelper.AlarmEntry.COLUMN_IS_NEXT_ALARM + " = 0" +
                   " WHERE " + SQLiteDBHelper.AlarmEntry.COLUMN_IS_NEXT_ALARM + " = 1;");
        entry.isNextAlarm = true;
        save(entry, false);
        return entry;
    }

    public void dropData() {
        db.execSQL("delete from " + SQLiteDBHelper.AlarmEntry.TABLE_NAME + ";");
    }
}
