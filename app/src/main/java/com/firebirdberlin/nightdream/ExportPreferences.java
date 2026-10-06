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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.firebirdberlin.nightdream.models.SimpleTime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ExportPreferences {
    public static final String TAG = "ExportPreferences";
    public static final String EXPORT_FILE_PATTERN = "nightdream_backup_%s.ndb";
    public static final String DEBUG_EXPORT_FILE_PATTERN = "nightclock_preferences_export_%s.json";

    private final Context context;
    private final Executor backgroundExecutor;
    private final Executor mainExecutor;

    public ExportPreferences(Context context) {
        this.context = context;
        this.backgroundExecutor = Executors.newSingleThreadExecutor();
        this.mainExecutor = ContextCompat.getMainExecutor(context);
    }

    /**
     * Exports all preferences and alarms to a user-selected SAF file Uri (.ndb).
     */
    public void exportToFile(Uri destinationUri) {
        mainExecutor.execute(() -> Toast.makeText(context, R.string.export_preferences_file, Toast.LENGTH_SHORT).show());

        backgroundExecutor.execute(() -> {
            try {
                JSONObject rootJson = generateBackupJson(false); // false = include smart home (encrypted)
                OutputStream outputStream = context.getContentResolver().openOutputStream(destinationUri);
                if (outputStream != null) {
                    outputStream.write(rootJson.toString(2).getBytes(StandardCharsets.UTF_8));
                    outputStream.close();
                    mainExecutor.execute(() -> Toast.makeText(context, "Settings exported successfully", Toast.LENGTH_LONG).show());
                } else {
                    throw new IOException("Failed to open output stream for destination Uri.");
                }
            } catch (Exception ex) {
                Log.e(TAG, "Export to file failed", ex);
                mainExecutor.execute(() -> Toast.makeText(context, "Failed to export settings", Toast.LENGTH_LONG).show());
            }
        });
    }

    /**
     * Sends debug preferences and device information via email / chooser (smart home exempted).
     */
    public void sendViaEmail() {
        mainExecutor.execute(() -> Toast.makeText(context, R.string.export_preferences_email, Toast.LENGTH_SHORT).show());

        backgroundExecutor.execute(() -> {
            File exportPath = new File(context.getFilesDir(), "export");
            if (!exportPath.exists()) {
                boolean created = exportPath.mkdirs();
                Log.d(TAG, "Export directory created: " + created);
            }

            // Delete old exports
            File[] oldFiles = exportPath.listFiles();
            if (oldFiles != null) {
                for (File oldFile : oldFiles) {
                    if (oldFile.isFile() && oldFile.getName().startsWith("nightclock_preferences_export")) {
                        oldFile.delete();
                    }
                }
            }

            long currentTime = System.currentTimeMillis();
            String exportDate = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(new Date(currentTime));
            String exportFileName = String.format(DEBUG_EXPORT_FILE_PATTERN, exportDate);

            File newFile = new File(exportPath, exportFileName);

            try {
                JSONObject rootJson = generateBackupJson(true); // true = exempt smart home settings
                FileOutputStream outputStream = new FileOutputStream(newFile);
                outputStream.write(rootJson.toString(2).getBytes(StandardCharsets.UTF_8));
                outputStream.close();

                mainExecutor.execute(() -> {
                    Uri contentUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileprovider", newFile);
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_STREAM, contentUri);
                    share.putExtra(Intent.EXTRA_SUBJECT, "NightDream App Information");
                    context.startActivity(Intent.createChooser(share, context.getString(R.string.export_preferences_email)));
                });

            } catch (Exception ex) {
                Log.e(TAG, "Email export failed", ex);
                mainExecutor.execute(() -> Toast.makeText(context, R.string.import_preferences_failed, Toast.LENGTH_LONG).show());
            }
        });
    }

    /**
     * Legacy entry point for export.
     */
    public void executeExport() {
        sendViaEmail();
    }

    private JSONObject generateBackupJson(boolean exemptSmartHome) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("version", 1);

        // Metadata
        JSONObject metadata = new JSONObject();
        metadata.put("appVersionCode", BuildConfig.VERSION_CODE);
        metadata.put("appVersionName", BuildConfig.VERSION_NAME);
        metadata.put("SDK", Build.VERSION.SDK_INT);
        metadata.put("Product", Build.PRODUCT);
        metadata.put("Manufacturer", Build.MANUFACTURER);
        metadata.put("Model", Build.MODEL);
        metadata.put("Device", Build.DEVICE);
        metadata.put("Display", Build.DISPLAY);
        metadata.put("Timezone", TimeZone.getDefault().getID());
        metadata.put("Offset", TimeZone.getDefault().getOffset(System.currentTimeMillis()));
        metadata.put("ExportTime", System.currentTimeMillis());
        root.put("metadata", metadata);

        // Preferences
        JSONObject allPreferencesObj = new JSONObject();

        List<String> prefNames = getSharedPreferencesNames();
        for (String prefName : prefNames) {
            SharedPreferences sp = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
            Map<String, ?> allEntries = sp.getAll();

            JSONObject prefEntryObj = new JSONObject();
            JSONObject typesObj = new JSONObject();
            JSONObject valuesObj = new JSONObject();

            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String key = entry.getKey();
                if (key == null) continue;

                // Exempt smart home keys for email debug export
                if (exemptSmartHome && key.startsWith("smart_home_")) {
                    continue;
                }

                Object value = entry.getValue();
                if (value == null) continue;

                if (value instanceof Boolean) {
                    typesObj.put(key, "boolean");
                    valuesObj.put(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    typesObj.put(key, "int");
                    valuesObj.put(key, (Integer) value);
                } else if (value instanceof Long) {
                    typesObj.put(key, "long");
                    valuesObj.put(key, (Long) value);
                } else if (value instanceof Float) {
                    typesObj.put(key, "float");
                    valuesObj.put(key, ((Float) value).doubleValue());
                } else if (value instanceof String) {
                    typesObj.put(key, "string");
                    valuesObj.put(key, (String) value);
                } else if (value instanceof Set) {
                    typesObj.put(key, "set");
                    JSONArray arr = new JSONArray();
                    for (Object item : (Set<?>) value) {
                        arr.put(item.toString());
                    }
                    valuesObj.put(key, arr);
                }
            }

            prefEntryObj.put("types", typesObj);
            prefEntryObj.put("values", valuesObj);
            allPreferencesObj.put(prefName, prefEntryObj);
        }

        root.put("preferences", allPreferencesObj);

        // Alarms
        JSONArray alarmsArray = new JSONArray();
        DataSource dataSource = new DataSource(context);
        try {
            dataSource.open();
            List<SimpleTime> alarms = dataSource.getAlarms();
            if (alarms != null) {
                for (SimpleTime alarm : alarms) {
                    JSONObject alarmObj = new JSONObject();
                    alarmObj.put("hour", alarm.hour);
                    alarmObj.put("min", alarm.min);
                    alarmObj.put("recurringDays", alarm.recurringDays);
                    alarmObj.put("isActive", alarm.isActive);
                    alarmObj.put("isNextAlarm", alarm.isNextAlarm);
                    alarmObj.put("radioStationIndex", alarm.radioStationIndex);
                    alarmObj.put("soundUri", alarm.soundUri != null ? alarm.soundUri : "");
                    alarmObj.put("vibrate", alarm.vibrate);
                    alarmObj.put("numAutoSnoozeCycles", alarm.numAutoSnoozeCycles);
                    if (alarm.name != null) {
                        alarmObj.put("name", alarm.name);
                    }
                    if (alarm.nextEventAfter != null) {
                        alarmObj.put("nextEventAfter", alarm.nextEventAfter);
                    }
                    alarmsArray.put(alarmObj);
                }
            }
        } finally {
            dataSource.close();
        }

        root.put("alarms", alarmsArray);

        return root;
    }

    private List<String> getSharedPreferencesNames() {
        Set<String> names = new HashSet<>();
        // Known preference files
        names.add(Settings.PREFS_KEY);
        names.add("defaults");
        names.add("DEFAULT");
        names.add("SIMPLE");
        names.add("ARC");
        names.add("MINIMALISTIC");

        // Scan shared_prefs directory for any dynamic files (like widgets)
        File dataDir = context.getDataDir();
        File prefsDir = new File(dataDir, "shared_prefs");

        if (prefsDir.exists() && prefsDir.isDirectory()) {
            File[] files = prefsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (fileName.endsWith(".xml")) {
                        String name = fileName.substring(0, fileName.length() - 4);
                        names.add(name);
                    }
                }
            }
        }

        return new ArrayList<>(names);
    }
}
