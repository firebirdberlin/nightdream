package com.firebirdberlin.nightdream;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class SQLiteDBHelper extends SQLiteOpenHelper {
    Context context;
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "sqlite.db";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + AlarmEntry.TABLE_NAME + " (" +
                    AlarmEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    AlarmEntry.COLUMN_HOUR + " INTEGER NOT NULL," +
                    AlarmEntry.COLUMN_MINUTE + " INTEGER NOT NULL," +
                    AlarmEntry.COLUMN_DAYS + " INTEGER DEFAULT 0," +
                    AlarmEntry.COLUMN_IS_ACTIVE + " INTEGER DEFAULT 0," +
                    AlarmEntry.COLUMN_IS_NEXT_ALARM + " INTEGER DEFAULT 0," +
                    AlarmEntry.COLUMN_ALARM_SOUND_URI + " text" +
                    ")";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + AlarmEntry.TABLE_NAME;

    public SQLiteDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion <= 2) {
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        } else if (newVersion == 3 && oldVersion < 3) {
            db.execSQL("ALTER TABLE " + AlarmEntry.TABLE_NAME + " ADD COLUMN " + AlarmEntry.COLUMN_ALARM_SOUND_URI + " text");
            // migrate the current setting of the alarm tone uri
            Settings settings = new Settings(this.context);
            if (settings.AlarmToneUri != null && !settings.AlarmToneUri.isEmpty()) {
                db.execSQL("UPDATE " + AlarmEntry.TABLE_NAME + " SET " +
                        AlarmEntry.COLUMN_ALARM_SOUND_URI + " = '" + settings.AlarmToneUri + "';");
            }

        }
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /* Inner class that defines the table contents */
    public static class AlarmEntry implements BaseColumns {
        public static final String TABLE_NAME = "alarms";
        public static final String COLUMN_HOUR = "hour";
        public static final String COLUMN_MINUTE = "minute";
        public static final String COLUMN_DAYS = "days";
        public static final String COLUMN_IS_ACTIVE = "isActive";
        public static final String COLUMN_IS_NEXT_ALARM = "isNextAlarm";
        public static final String COLUMN_ALARM_SOUND_URI = "alarmSoundUri";
    }
}