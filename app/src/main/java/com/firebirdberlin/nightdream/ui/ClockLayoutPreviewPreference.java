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

package com.firebirdberlin.nightdream.ui;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.firebirdberlin.nightdream.PreferencesActivity;
import com.firebirdberlin.nightdream.PurchaseManager;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.AnalogClockConfig;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

public class ClockLayoutPreviewPreference extends Preference {
    private static final String TAG = "ClockLayoutPreviewPreference";
    private static PreviewMode previewMode = PreviewMode.DAY;
    private final Context context;
    private ClockLayout clockLayout = null;
    private TextView textViewPurchaseHint = null;
    private View preferenceView = null;
    private LinearLayout preferencesContainer = null;
    private ImageButton resetButton = null;
    private ImageButton exportButton = null;
    private ImageButton importButton = null;
    private Runnable importAction = null;

    public void setImportAction(Runnable action) {
        this.importAction = action;
    }

    public ClockLayoutPreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public ClockLayoutPreviewPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    public static void setPreviewMode(PreviewMode previewMode) {
        ClockLayoutPreviewPreference.previewMode = previewMode;
    }

    public void invalidate() {
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        preferenceView = holder.itemView;

        View summary = preferenceView.findViewById(android.R.id.summary);
        if (summary != null) {
            ViewParent summaryParent = summary.getParent();
            if (summaryParent instanceof ViewGroup) {
                final LayoutInflater layoutInflater =
                        (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                ViewGroup summaryParent2 = (ViewGroup) summaryParent;
                View view = summaryParent2.findViewWithTag("custom");
                if (view == null) {
                    layoutInflater.inflate(R.layout.clock_layout_preference, summaryParent2, true);
                }

                RelativeLayout previewContainer = summaryParent2.findViewById(R.id.previewContainer);
                clockLayout = summaryParent2.findViewById(R.id.clockLayout);
                resetButton = summaryParent2.findViewById(R.id.resetButton);
                exportButton = summaryParent2.findViewById(R.id.exportButton);
                importButton = summaryParent2.findViewById(R.id.importButton);
                textViewPurchaseHint = summaryParent2.findViewById(R.id.textViewPurchaseHint);
                preferencesContainer = summaryParent2.findViewById(R.id.preferencesContainer);

                LayoutTransition lt = new LayoutTransition();
                lt.disableTransitionType(LayoutTransition.CHANGING);
                previewContainer.setLayoutTransition(lt);
            }
        }
        updateView();
    }

    protected void updateView() {
        Settings settings = new Settings(getContext());
        int clockLayoutId = settings.getClockLayoutID(true);
        setupPurchaseHint(settings);
        boolean showButtons = showResetButton(settings);
        resetButton.setVisibility(showButtons ? View.VISIBLE : View.GONE);
        exportButton.setVisibility(showButtons ? View.VISIBLE : View.GONE);
        importButton.setVisibility(showButtons ? View.VISIBLE : View.GONE);
        updateClockLayout(clockLayoutId, settings);
        setupPreferencesFragment(clockLayoutId, settings);
        setupResetButton(clockLayoutId);
        setupExportButton(clockLayoutId);
        setupImportButton();
    }

    private void updateClockLayout(int clockLayoutId, Settings settings) {
        int color = previewMode == PreviewMode.DAY ? settings.clockColor : settings.clockColorNight;
        int glowRadius = settings.getGlowRadius(clockLayoutId);
        int textureId = settings.getTextureResId(clockLayoutId);
        clockLayout.setPrimaryColor(color);
        clockLayout.setLayout(clockLayoutId);
        clockLayout.setBackgroundColor(Color.TRANSPARENT);
        clockLayout.setTypeface(settings.loadTypeface());
        clockLayout.setPrimaryColor(color, glowRadius, color, textureId, false);
        clockLayout.setSecondaryColor(previewMode == PreviewMode.DAY ? settings.secondaryColor : settings.secondaryColorNight);
        clockLayout.setColorHours(settings.getColorHours(clockLayoutId));
        clockLayout.setColorMinutes(settings.getColorMinutes(clockLayoutId));
        clockLayout.setColorSeconds(settings.getColorSeconds(clockLayoutId));

        clockLayout.setDateFormat(settings.dateFormat);
        clockLayout.setTimeFormat(settings.getTimeFormat(clockLayoutId), settings.is24HourFormat());
        clockLayout.setShowDivider(settings.getShowDivider(clockLayoutId));
        clockLayout.setMirrorText(settings.clockLayoutMirrorText);
        clockLayout.setScaleFactor(1.f);
        clockLayout.showDate(settings.showDate);
        clockLayout.showCalendarEvents(settings.getShowCalendarEvents(clockLayoutId));
        clockLayout.setWeatherIconSizeFactor(settings.getWeatherIconSizeFactor(clockLayoutId));

        clockLayout.setTemperature(settings.showTemperature, settings.showApparentTemperature, settings.temperatureUnit);
        clockLayout.setWindSpeed(settings.showWindSpeed, settings.speedUnit);
        clockLayout.setWeatherLocation(false);
        clockLayout.setWeatherIconMode(settings.weather_icon);
        clockLayout.showWeather(settings.shallShowWeather());
        clockLayout.setShowNotifications(false);
        clockLayout.showPollenExposure(false);

        WeatherEntry entry = getWeatherEntry(settings);
        clockLayout.update(entry, false);

        Point size = Utility.getDisplaySize(getContext());
        Configuration config = context.getResources().getConfiguration();
        clockLayout.updateLayout(
                size.x - preferenceView.getPaddingLeft() - preferenceView.getPaddingRight(),
                config
        );

        clockLayout.requestLayout();
        clockLayout.invalidate();
    }

    private void setupPreferencesFragment(final int clockLayoutID, final Settings settings) {
        preferencesContainer.removeAllViews();

        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        switch (clockLayoutID) {
            case ClockLayout.LAYOUT_ID_DIGITAL:
            case ClockLayout.LAYOUT_ID_DIGITAL2:
            case ClockLayout.LAYOUT_ID_DIGITAL3:
                CustomDigitalClockPreferencesLayout prefs_digital =
                        new CustomDigitalClockPreferencesLayout(context, settings, getActivity(), clockLayoutID);
                prefs_digital.setIsPurchased(purchased(PurchaseManager.ITEM_WEATHER_DATA));
                prefs_digital.setOnConfigChangedListener(
                        new CustomDigitalClockPreferencesLayout.OnConfigChangedListener() {
                            @Override
                            public void onConfigChanged() {
                                updateView();
                            }

                            @Override
                            public void onPurchaseRequested() {
                                ((PreferencesActivity) context).showSubscriptionDialog();
                            }
                        }
                );
                preferencesContainer.addView(prefs_digital, lp);
                break;
            case ClockLayout.LAYOUT_ID_CALENDAR:
                CustomCalendarClockPreferencesLayout prefs_calendar =
                        new CustomCalendarClockPreferencesLayout(context, settings, getActivity());
                prefs_calendar.setIsPurchased(purchased(PurchaseManager.ITEM_WEATHER_DATA));
                prefs_calendar.setOnConfigChangedListener(
                        new CustomCalendarClockPreferencesLayout.OnConfigChangedListener() {
                            @Override
                            public void onConfigChanged() {
                                updateView();
                            }

                            @Override
                            public void onPurchaseRequested() {
                                ((PreferencesActivity) context).showSubscriptionDialog();
                            }
                        }
                );
                preferencesContainer.addView(prefs_calendar, lp);
                break;
            case ClockLayout.LAYOUT_ID_DIGITAL_ANIMATED:
                CustomAnimClockPreferencesLayout prefs_anim =
                        new CustomAnimClockPreferencesLayout(context, settings, getActivity());
                prefs_anim.setOnConfigChangedListener(
                        new CustomAnimClockPreferencesLayout.OnConfigChangedListener() {
                            @Override
                            public void onConfigChanged() {
                                updateView();
                            }

                            @Override
                            public void onPurchaseRequested() {
                                ((PreferencesActivity) context).showSubscriptionDialog();
                            }
                        }
                );
                preferencesContainer.addView(prefs_anim, lp);
                break;
            case ClockLayout.LAYOUT_ID_ANALOG2:
            case ClockLayout.LAYOUT_ID_ANALOG3:
            case ClockLayout.LAYOUT_ID_ANALOG4:
                AnalogClockConfig.Style preset = AnalogClockConfig.toClockStyle(clockLayoutID);
                CustomAnalogClockPreferencesLayout prefs_analog =
                        new CustomAnalogClockPreferencesLayout(context, preset, getActivity());

                prefs_analog.setIsPurchased(purchased(PurchaseManager.ITEM_WEATHER_DATA));
                prefs_analog.setOnConfigChangedListener(
                        new CustomAnalogClockPreferencesLayout.OnConfigChangedListener() {
                            @Override
                            public void onConfigChanged() {
                                updateClockLayout(clockLayoutID, settings);
                            }

                            @Override
                            public void onPurchaseRequested() {
                                ((PreferencesActivity) context).showPurchaseDialog();
                            }
                        }
                );
                preferencesContainer.addView(prefs_analog, lp);
                break;
        }
    }

    private void setupResetButton(final int clockLayoutID) {
        resetButton.setOnClickListener(v -> {
            Context context = getContext();
            Resources res = context.getResources();
            new AlertDialog.Builder(context)
                    .setTitle(res.getString(R.string.confirm_reset))
                    .setMessage(res.getString(R.string.confirm_reset_question_layout))
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        AnalogClockConfig.Style preset = AnalogClockConfig.toClockStyle(clockLayoutID);
                        AnalogClockConfig config = new AnalogClockConfig(getContext(), preset);
                        config.reset();
                        updateView();
                    }).show();
        });

    }

    private void setupExportButton(final int clockLayoutID) {
        exportButton.setOnClickListener(v -> {
            Context context = getContext();
            AnalogClockConfig.Style preset = AnalogClockConfig.toClockStyle(clockLayoutID);
            AnalogClockConfig config = new AnalogClockConfig(context, preset);
            String json = config.toJson();

            File exportPath = new File(context.getFilesDir(), "export");
            if (!exportPath.exists()) {
                boolean ignored = exportPath.mkdirs();
            }
            String fileName = "nightdream_clock_" + preset.name().toLowerCase() + "_" + System.currentTimeMillis() + ".json";
            File file = new File(exportPath, fileName);
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(json.getBytes());
                fos.close();

                Uri contentUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileprovider", file);
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("application/json");
                share.putExtra(Intent.EXTRA_STREAM, contentUri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(Intent.createChooser(share, "Export"));
            } catch (IOException e) {
                Log.e(TAG, "Export failed", e);
                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupImportButton() {
        importButton.setOnClickListener(v -> {
            if (importAction != null) {
                importAction.run();
            } else {
                Toast.makeText(context, "Import not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void importConfigFromUri(Uri uri) {
        Context ctx = getContext();
        try {
            InputStream inputStream = ctx.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                inputStream.close();

                Settings settings = new Settings(ctx);
                int clockLayoutId = settings.getClockLayoutID(true);
                AnalogClockConfig.Style preset = AnalogClockConfig.toClockStyle(clockLayoutId);
                AnalogClockConfig config = new AnalogClockConfig(ctx, preset);
                config.importFromJson(stringBuilder.toString());
                updateView();
                Toast.makeText(ctx, "Settings imported successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Import failed", e);
            Toast.makeText(ctx, "Import failed: invalid file", Toast.LENGTH_SHORT).show();
        }
    }

    private WeatherEntry getWeatherEntry(Settings settings) {
        WeatherEntry entry = settings.getWeatherEntry();
        if (!entry.isValid()) {
            entry.setFakeData();
        }
        return entry;
    }

    private void setupPurchaseHint(Settings settings) {
        int layoutID = settings.getClockLayoutID(true);
        if (layoutID == ClockLayout.LAYOUT_ID_CALENDAR
                && !purchased(PurchaseManager.ITEM_WEATHER_DATA)) {
            textViewPurchaseHint.setText(getContext().getString(R.string.product_name_pro));
            textViewPurchaseHint.setVisibility(View.VISIBLE);

        } else if (layoutID >= ClockLayout.LAYOUT_ID_ANALOG2
                && !purchased(PurchaseManager.ITEM_WEATHER_DATA)) {
            textViewPurchaseHint.setText(getContext().getString(R.string.product_name_pro));
            textViewPurchaseHint.setVisibility(View.VISIBLE);

        } else {
            textViewPurchaseHint.setVisibility(View.GONE);
        }
        textViewPurchaseHint.invalidate();
    }

    private boolean purchased(String sku) {
        PreferencesActivity preferencesActivity = (PreferencesActivity) getActivity();
        if (preferencesActivity != null) {
            return preferencesActivity.isPurchased(sku);
        }
        return false;
    }

    private AppCompatActivity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof AppCompatActivity) {
                return (AppCompatActivity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private boolean showResetButton(Settings settings) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return false;
        }

        int layoutID = settings.getClockLayoutID(true);
        return (
                layoutID != ClockLayout.LAYOUT_ID_ANALOG
                        && layoutID != ClockLayout.LAYOUT_ID_DIGITAL
                        && layoutID != ClockLayout.LAYOUT_ID_DIGITAL2
                        && layoutID != ClockLayout.LAYOUT_ID_DIGITAL3
                        && layoutID != ClockLayout.LAYOUT_ID_DIGITAL_FLIP
                        && layoutID != ClockLayout.LAYOUT_ID_CALENDAR
                        && layoutID != ClockLayout.LAYOUT_ID_DIGITAL_ANIMATED
        );
    }

    public enum PreviewMode {DAY, NIGHT}
}
