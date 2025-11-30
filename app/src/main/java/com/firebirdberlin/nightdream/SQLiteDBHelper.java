/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.firebirdberlin.nightdream;


import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import org.json.JSONException;

public class SQLiteDBHelper extends SQLiteOpenHelper {
    Context context;
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 8;
    private static final String DATABASE_NAME = "sqlite.db";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + AlarmEntry.TABLE_NAME + " (" +
                    AlarmEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    AlarmEntry.COLUMN_HOUR + " INTEGER NOT NULL, " +
                    AlarmEntry.COLUMN_MINUTE + " INTEGER NOT NULL, " +
                    AlarmEntry.COLUMN_DAYS + " INTEGER DEFAULT 0, " +
                    AlarmEntry.COLUMN_IS_ACTIVE + " INTEGER DEFAULT 0, " +
                    AlarmEntry.COLUMN_IS_NEXT_ALARM + " INTEGER DEFAULT 0, " +
                    AlarmEntry.COLUMN_ALARM_SOUND_URI + " text, " +
                    AlarmEntry.COLUMN_NEXT_EVENT_AFTER + " INTEGER DEFAULT NULL, " +
                    AlarmEntry.COLUMN_RADIO_STATION_INDEX + " INTEGER DEFAULT -1, " +
                    AlarmEntry.COLUMN_VIBRATE + " INTEGER DEFAULT 0, " +
                    AlarmEntry.COLUMN_NUM_AUTO_SNOOZE_CYCLES + " INTEGER DEFAULT 0," +
                    AlarmEntry.COLUMN_NAME + " text" +
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
            return;
        }

        if (newVersion >= 3 && oldVersion < 3) {
            db.execSQL("ALTER TABLE " + AlarmEntry.TABLE_NAME + " ADD COLUMN " + AlarmEntry.COLUMN_ALARM_SOUND_URI + " text");
            // migrate the current setting of the alarm tone uri
            Settings settings = new Settings(this.context);
            if (settings.AlarmToneUri != null && !settings.AlarmToneUri.isEmpty()) {
                db.execSQL("UPDATE " + AlarmEntry.TABLE_NAME + " SET " +
                        AlarmEntry.COLUMN_ALARM_SOUND_URI + " = '" + settings.AlarmToneUri + "';");
            }

        }

        if (newVersion >= 4 && oldVersion < 4) {
            db.execSQL("ALTER TABLE " + AlarmEntry.TABLE_NAME + " ADD COLUMN " + AlarmEntry.COLUMN_NEXT_EVENT_AFTER + " INTEGER DEFAULT NULL");
        }

        if (newVersion >= 5 && oldVersion < 5) {
            db.execSQL("ALTER TABLE " + AlarmEntry.TABLE_NAME + " ADD COLUMN " + AlarmEntry.COLUMN_RADIO_STATION_INDEX + " INTEGER DEFAULT -1");
            updateAlarmClockRadioStation(db);
        }

        if (newVersion >= 6 && oldVersion < 6) {
            db.execSQL("ALTER TABLE " + AlarmEntry.TABLE_NAME + " ADD COLUMN " + AlarmEntry.COLUMN_VIBRATE + " INTEGER DEFAULT 0");
        }

        if (newVersion >= 7 && oldVersion < 7) {
            db.execSQL("ALTER TABLE " + AlarmEntry.TABLE_NAME + " ADD COLUMN " + AlarmEntry.COLUMN_NUM_AUTO_SNOOZE_CYCLES + " INTEGER DEFAULT 0");
        }

        if (newVersion >= 8 && oldVersion < 8) {
            db.execSQL("ALTER TABLE " + AlarmEntry.TABLE_NAME + " ADD COLUMN " + AlarmEntry.COLUMN_NAME + " text");
        }
    }

    private void updateAlarmClockRadioStation(SQLiteDatabase db) {
        // do the migration for alarms
        // this function can be removed when versions <= 1.9.5 are no longer relevant
        SharedPreferences prefs = context.getSharedPreferences(Settings.PREFS_KEY, 0);
        boolean useRadioAlarmClock = prefs.getBoolean("useRadioAlarmClock", false);
        if (!useRadioAlarmClock) return;

        RadioStation alarmRadioStation = getAlarmRadioStation(prefs);

        if (alarmRadioStation == null) return;

        Settings settings = new Settings(this.context);
        FavoriteRadioStations stations = settings.getFavoriteRadioStations();

        int index = -1;
        if (alarmRadioStation.stream != null && stations != null) {
            for(int i = 0; i < stations.numAvailableStations(); i++) {
                RadioStation station = stations.get(i);
                if (station != null) {
                    if (alarmRadioStation.stream.equals(station.stream)) {
                        index = i;
                        break;
                    }
                }
            }
        }

        if (index == -1 && stations != null) {
            index = stations.numAvailableStations();
            stations.set(index, alarmRadioStation);
        }

        if (index > -1 && index < FavoriteRadioStations.MAX_NUM_ENTRIES) {
            // set this index to the db entry
            db.execSQL("UPDATE " + AlarmEntry.TABLE_NAME + " SET " +
                    AlarmEntry.COLUMN_RADIO_STATION_INDEX + " = '" + String.valueOf(index) + "';");

        }

    }

    private RadioStation getAlarmRadioStation(SharedPreferences prefs) {
        String json = prefs.getString("radioStreamURL_json",  null);
        if (json != null) {
            try {
                return RadioStation.fromJson(json);
            } catch (JSONException e) {}
        }
        return null;
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
        public static final String COLUMN_NEXT_EVENT_AFTER = "nextEventAfter";
        public static final String COLUMN_RADIO_STATION_INDEX = "radioStationIndex";
        public static final String COLUMN_NUM_AUTO_SNOOZE_CYCLES = "numAutoSnoozeCycles";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_VIBRATE = "vibrate";
    }
}