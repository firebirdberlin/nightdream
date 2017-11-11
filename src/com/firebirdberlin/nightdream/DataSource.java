package com.firebirdberlin.nightdream;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.firebirdberlin.nightdream.models.SimpleTime;

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
        if (time.id == -1L) {
            return insert(time);
        } else {
            return update(time);
        }
    }

    private SimpleTime insert(SimpleTime time) {
        ContentValues values = toContentValues(time);
        if (db == null) return null;
        long new_id = db.insert(SQLiteDBHelper.AlarmEntry.TABLE_NAME, null, values);
        time.id = new_id;
        return time;
    }

    private SimpleTime update(SimpleTime time) {
        ContentValues values = toContentValues(time);

        String selection = SQLiteDBHelper.AlarmEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(time.id)};

        db.update(SQLiteDBHelper.AlarmEntry.TABLE_NAME, values, selection, selectionArgs);
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
        return values;
    }

    public void delete(SimpleTime time) {
        String selection = SQLiteDBHelper.AlarmEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(time.id)};

        db.delete(SQLiteDBHelper.AlarmEntry.TABLE_NAME, selection, selectionArgs);
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

    private Cursor getQueryCursor(String where, String[] whereArgs) {
        String[] projection = {
                SQLiteDBHelper.AlarmEntry._ID,
                SQLiteDBHelper.AlarmEntry.COLUMN_HOUR,
                SQLiteDBHelper.AlarmEntry.COLUMN_MINUTE,
                SQLiteDBHelper.AlarmEntry.COLUMN_DAYS,
                SQLiteDBHelper.AlarmEntry.COLUMN_IS_ACTIVE,
                SQLiteDBHelper.AlarmEntry.COLUMN_IS_NEXT_ALARM
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
        return time;
    }

    public SimpleTime setNextAlarm(SimpleTime entry) {
        db.execSQL("UPDATE " + SQLiteDBHelper.AlarmEntry.TABLE_NAME +
                   " SET " + SQLiteDBHelper.AlarmEntry.COLUMN_IS_NEXT_ALARM + " = 0" +
                   " WHERE " + SQLiteDBHelper.AlarmEntry.COLUMN_IS_NEXT_ALARM + " = 1;");
        entry.isNextAlarm = true;
        save(entry);
        return entry;
    }

    public void dropData() {
        db.execSQL("delete from " + SQLiteDBHelper.AlarmEntry.TABLE_NAME + ";");
    }
}
