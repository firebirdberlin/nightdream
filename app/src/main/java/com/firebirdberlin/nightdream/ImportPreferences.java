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
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.services.SqliteIntentService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ImportPreferences {
    public static final String TAG = "ImportPreferences";

    private final Context context;
    private final Executor backgroundExecutor;
    private final Executor mainExecutor;

    public ImportPreferences(Context context) {
        this.context = context;
        this.backgroundExecutor = Executors.newSingleThreadExecutor();
        this.mainExecutor = ContextCompat.getMainExecutor(context);
    }

    public boolean canImport() {
        if ("noGms".equalsIgnoreCase(BuildConfig.FLAVOR)) {
            return true;
        }
        PurchaseManager pm = PurchaseManager.getInstance(context);
        return pm.isPurchased(PurchaseManager.ITEM_ACTIONS) || pm.isPurchased(PurchaseManager.ITEM_ONE_YEAR_SUBSCRIPTION);
    }

    public void executeImport(Uri fileUri, Runnable onComplete) {
        if (!canImport()) {
            Toast.makeText(context, R.string.import_preferences_requires_pro, Toast.LENGTH_LONG).show();
            return;
        }

        mainExecutor.execute(() -> Toast.makeText(context, R.string.import_preferences, Toast.LENGTH_SHORT).show());

        backgroundExecutor.execute(() -> {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
                if (inputStream == null) {
                    throw new IllegalArgumentException("Cannot open input stream for Uri: " + fileUri);
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                inputStream.close();

                JSONObject rootJson = new JSONObject(stringBuilder.toString());

                // Restore SharedPreferences
                if (rootJson.has("preferences")) {
                    JSONObject preferencesObj = rootJson.getJSONObject("preferences");
                    importSharedPreferences(preferencesObj);
                }

                // Restore Alarms
                if (rootJson.has("alarms")) {
                    JSONArray alarmsArray = rootJson.getJSONArray("alarms");
                    importAlarms(alarmsArray);
                }

                // Reschedule alarms after database update
                SqliteIntentService.scheduleAlarm(context);

                mainExecutor.execute(() -> {
                    Toast.makeText(context, R.string.import_preferences_success, Toast.LENGTH_LONG).show();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });

            } catch (Exception ex) {
                Log.e(TAG, "Import failed", ex);
                mainExecutor.execute(() -> Toast.makeText(context, R.string.import_preferences_failed, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void importSharedPreferences(JSONObject preferencesObj) throws JSONException {
        Iterator<String> prefNames = preferencesObj.keys();
        while (prefNames.hasNext()) {
            String prefName = prefNames.next();
            JSONObject prefEntryObj = preferencesObj.getJSONObject(prefName);

            JSONObject typesObj = prefEntryObj.optJSONObject("types");
            JSONObject valuesObj = prefEntryObj.optJSONObject("values");

            if (valuesObj == null) {
                valuesObj = prefEntryObj; // Fallback if no separate types/values object
            }

            SharedPreferences sp = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);

            // Preserve local purchase status cache in "defaults"
            Map<String, Boolean> purchaseCache = new HashMap<>();
            if ("defaults".equals(prefName)) {
                Map<String, ?> all = sp.getAll();
                for (Map.Entry<String, ?> entry : all.entrySet()) {
                    if (entry.getKey().startsWith("purchased_") && entry.getValue() instanceof Boolean) {
                        purchaseCache.put(entry.getKey(), (Boolean) entry.getValue());
                    }
                }
            }

            SharedPreferences.Editor editor = sp.edit();
            editor.clear();
            editor.commit();

            Iterator<String> keys = valuesObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();

                // Skip local billing cache entries
                if ("defaults".equals(prefName) && key.startsWith("purchased_")) {
                    continue;
                }

                String type = (typesObj != null && typesObj.has(key)) ? typesObj.getString(key) : null;
                Object valueObj = valuesObj.get(key);

                if ("boolean".equals(type) || valueObj instanceof Boolean) {
                    editor.putBoolean(key, valuesObj.getBoolean(key));
                } else if ("int".equals(type) || (valueObj instanceof Integer && type == null)) {
                    editor.putInt(key, valuesObj.getInt(key));
                } else if ("long".equals(type) || valueObj instanceof Long) {
                    editor.putLong(key, valuesObj.getLong(key));
                } else if ("float".equals(type) || valueObj instanceof Double) {
                    editor.putFloat(key, (float) valuesObj.getDouble(key));
                } else if ("set".equals(type) || valueObj instanceof JSONArray) {
                    JSONArray arr = valuesObj.getJSONArray(key);
                    Set<String> set = new HashSet<>();
                    for (int i = 0; i < arr.length(); i++) {
                        set.add(arr.getString(i));
                    }
                    editor.putStringSet(key, set);
                } else {
                    editor.putString(key, valuesObj.getString(key));
                }
            }

            // Restore local purchase status cache
            for (Map.Entry<String, Boolean> entry : purchaseCache.entrySet()) {
                editor.putBoolean(entry.getKey(), entry.getValue());
            }

            editor.commit();
        }
    }

    private void importAlarms(JSONArray alarmsArray) throws JSONException {
        DataSource dataSource = new DataSource(context);
        try {
            dataSource.open();
            dataSource.dropData(); // Clear existing alarms

            for (int i = 0; i < alarmsArray.length(); i++) {
                JSONObject alarmObj = alarmsArray.getJSONObject(i);
                SimpleTime time = new SimpleTime();
                time.id = -1L;
                time.hour = alarmObj.optInt("hour", 0);
                time.min = alarmObj.optInt("min", 0);
                time.recurringDays = alarmObj.optInt("recurringDays", 0);
                time.isActive = alarmObj.optBoolean("isActive", false);
                time.isNextAlarm = alarmObj.optBoolean("isNextAlarm", false);
                time.radioStationIndex = alarmObj.optInt("radioStationIndex", -1);
                time.soundUri = alarmObj.optString("soundUri", null);
                time.vibrate = alarmObj.optBoolean("vibrate", false);
                time.numAutoSnoozeCycles = alarmObj.optInt("numAutoSnoozeCycles", 0);
                if (alarmObj.has("name") && !alarmObj.isNull("name")) {
                    time.name = alarmObj.getString("name");
                }
                if (alarmObj.has("nextEventAfter") && !alarmObj.isNull("nextEventAfter")) {
                    time.nextEventAfter = alarmObj.getLong("nextEventAfter");
                }

                dataSource.save(time);
            }
        } finally {
            dataSource.close();
        }
    }
}
