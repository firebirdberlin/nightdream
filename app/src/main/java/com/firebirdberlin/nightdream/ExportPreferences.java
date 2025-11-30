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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ExportPreferences extends AsyncTask<Void, Void, Void> {
    public static final String TAG = "ExportPreferences";
    private static final String exportFile = "nightclock_preferences_export_%s.json";
    private Context context;

    public ExportPreferences(Context context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        Toast.makeText(context, "Starting Export", Toast.LENGTH_LONG).show();
    }

    @Override
    protected Void doInBackground(Void... params) {
        File exportPath = new File(context.getFilesDir(), "export");
        if (!exportPath.exists()) {
            boolean mkdirsResult = exportPath.mkdirs();
            Log.d(TAG, "Created Directory for export: " + mkdirsResult);
        }

        // delete old exports
        File[] oldFiles = exportPath.listFiles();
        if (oldFiles != null) {
            for (File oldFile : oldFiles) {
                if (oldFile.isFile() && oldFile.getName().startsWith("nightclock_preferences_export")) {
                    oldFile.delete();
                    Log.d(TAG, "Old files deleted: " + oldFile);
                }
            }
        }

        // Generate a file name
        long currentTime = System.currentTimeMillis();
        String exportDate = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(new Date(currentTime));
        String exportFileName = String.format(exportFile, exportDate);

        // Create a new file
        File newFile = new File(exportPath, exportFileName);

        try {
            // Start writing
            FileOutputStream outputStream = new FileOutputStream(newFile);
            outputStream.write("{\n\n".getBytes());

            // Device info
            outputStream.write("\"device\": ".getBytes());

            JSONObject json = new JSONObject();
            json.put("Version", BuildConfig.VERSION_CODE);
            json.put("SDK", Build.VERSION.SDK_INT);
            json.put("Product", Build.PRODUCT);
            json.put("Manufacturer", Build.MANUFACTURER);
            json.put("Model", Build.MODEL);
            json.put("Device", Build.DEVICE);
            json.put("Display", Build.DISPLAY);
            json.put("Timezone", TimeZone.getDefault().getID());
            json.put("Offset", TimeZone.getDefault().getOffset(System.currentTimeMillis()));
            json.put("ExportTime", System.currentTimeMillis());

            Map<String, ?> allSP = context.getSharedPreferences(Settings.PREFS_KEY, 0).getAll();
            for (Map.Entry<String, ?> entry : allSP.entrySet()) {
                json.put(entry.getKey(), entry.getValue().toString());
            }

            outputStream.write(json.toString().getBytes());
            outputStream.write("\n\n}".getBytes());
            outputStream.close();
        } catch (JSONException | IOException ex) {
            Toast.makeText(context, "Export failed.", Toast.LENGTH_LONG).show();
            return null;
        }

        Uri contentUri = FileProvider.getUriForFile(context, "nightdream", newFile);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_STREAM, contentUri);
        context.startActivity(Intent.createChooser(share, "Export"));
        return null;
    }
}