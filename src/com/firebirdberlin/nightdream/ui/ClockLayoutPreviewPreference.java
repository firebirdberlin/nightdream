package com.firebirdberlin.nightdream.ui;

import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
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

import com.firebirdberlin.nightdream.BillingHelperActivity;
import com.firebirdberlin.nightdream.PreferencesActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.AnalogClockConfig;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

public class ClockLayoutPreviewPreference extends Preference {
    private static final String TAG = "ClockLayoutPreviewPreference";
    private static PreviewMode previewMode = PreviewMode.DAY;
    private ClockLayout clockLayout = null;
    private TextView textViewPurchaseHint = null;
    private View preferenceView = null;
    private LinearLayout preferencesContainer = null;
    private ImageButton resetButton = null;

    private final Context context;

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
                textViewPurchaseHint = summaryParent2.findViewById(R.id.textViewPurchaseHint);
                preferencesContainer = summaryParent2.findViewById(R.id.preferencesContainer);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    LayoutTransition lt = new LayoutTransition();
                    lt.disableTransitionType(LayoutTransition.CHANGING);
                    previewContainer.setLayoutTransition(lt);
                }
            }
        }
        updateView();
    }

    protected void updateView() {
        Settings settings = new Settings(getContext());
        int clockLayoutId = settings.getClockLayoutID(true);
        setupPurchaseHint(settings);
        resetButton.setVisibility(showResetButton(settings) ? View.VISIBLE : View.GONE);
        updateClockLayout(clockLayoutId, settings);
        setupPreferencesFragment(clockLayoutId, settings);
        setupResetButton(clockLayoutId);
    }

    private void updateClockLayout(int clockLayoutId, Settings settings) {
        clockLayout.setLayout(clockLayoutId);
        clockLayout.setBackgroundColor(Color.TRANSPARENT);
        clockLayout.setTypeface(settings.typeface);
        int color = previewMode == PreviewMode.DAY ? settings.clockColor : settings.clockColorNight;
        int glowRadius = settings.getGlowRadius(clockLayoutId);
        int textureId = settings.getTextureResId(clockLayoutId);
        clockLayout.setPrimaryColor(color, glowRadius, color, textureId, false);
        clockLayout.setSecondaryColor(previewMode == PreviewMode.DAY ? settings.secondaryColor : settings.secondaryColorNight);

        clockLayout.setDateFormat(settings.dateFormat);
        clockLayout.setTimeFormat(settings.getTimeFormat(clockLayoutId), settings.is24HourFormat());
        clockLayout.setShowDivider(settings.getShowDivider(clockLayoutId));
        clockLayout.setMirrorText(settings.clockLayoutMirrorText);
        clockLayout.setScaleFactor(1.f);
        clockLayout.showDate(settings.showDate);
        clockLayout.setWeatherIconSizeFactor(settings.getWeatherIconSizeFactor(clockLayoutId));

        clockLayout.setTemperature(settings.showTemperature, settings.showApparentTemperature, settings.temperatureUnit);
        clockLayout.setWindSpeed(settings.showWindSpeed, settings.speedUnit);
        clockLayout.setWeatherLocation(false);
        clockLayout.setWeatherIconMode(settings.weather_icon);
        clockLayout.showWeather(settings.showWeather);
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
                prefs_digital.setIsPurchased(purchased(BillingHelperActivity.ITEM_WEATHER_DATA));
                prefs_digital.setOnConfigChangedListener(
                        new CustomDigitalClockPreferencesLayout.OnConfigChangedListener() {
                            @Override
                            public void onConfigChanged() {
                                updateView();
                            }

                            @Override
                            public void onPurchaseRequested() {
                                ((PreferencesActivity) context).showPurchaseDialog();
                            }
                        }
                );
                preferencesContainer.addView(prefs_digital, lp);
                break;
            case ClockLayout.LAYOUT_ID_CALENDAR:
                CustomCalendarClockPreferencesLayout prefs_calendar =
                        new CustomCalendarClockPreferencesLayout(context, settings, getActivity());
                prefs_calendar.setIsPurchased(purchased(BillingHelperActivity.ITEM_WEATHER_DATA));
                prefs_calendar.setOnConfigChangedListener(
                        new CustomCalendarClockPreferencesLayout.OnConfigChangedListener() {
                            @Override
                            public void onConfigChanged() {
                                updateView();
                            }

                            @Override
                            public void onPurchaseRequested() {
                                ((PreferencesActivity) context).showPurchaseDialog();
                            }
                        }
                );
                preferencesContainer.addView(prefs_calendar, lp);
                break;
            case ClockLayout.LAYOUT_ID_ANIM_DIGITAL:
                CustomAnimClockPreferencesLayout prefs_anim =
                        new CustomAnimClockPreferencesLayout(context, settings, getActivity());
                prefs_anim.setIsPurchased(purchased(BillingHelperActivity.ITEM_WEATHER_DATA));
                prefs_anim.setOnConfigChangedListener(
                        new CustomAnimClockPreferencesLayout.OnConfigChangedListener() {
                            @Override
                            public void onConfigChanged() {
                                updateView();
                            }

                            @Override
                            public void onPurchaseRequested() {
                                ((PreferencesActivity) context).showPurchaseDialog();
                            }
                        }
                );
                preferencesContainer.addView(prefs_anim, lp);
                break;
            case ClockLayout.LAYOUT_ID_ANALOG2:
            case ClockLayout.LAYOUT_ID_ANALOG3:
            case ClockLayout.LAYOUT_ID_ANALOG4:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    // the view is not drawn correctly. We have issues with invalidation.
                    return;
                }
                AnalogClockConfig.Style preset = AnalogClockConfig.toClockStyle(clockLayoutID);
                CustomAnalogClockPreferencesLayout prefs_analog =
                        new CustomAnalogClockPreferencesLayout(context, preset, getActivity());

                prefs_analog.setIsPurchased(purchased(BillingHelperActivity.ITEM_WEATHER_DATA));
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
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getContext();
                Resources res = context.getResources();
                new AlertDialog.Builder(context)
                        .setTitle(res.getString(R.string.confirm_reset))
                        .setMessage(res.getString(R.string.confirm_reset_question_layout))
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                AnalogClockConfig.Style preset = AnalogClockConfig.toClockStyle(clockLayoutID);
                                AnalogClockConfig config = new AnalogClockConfig(getContext(), preset);
                                config.reset();
                                updateView();
                            }
                        }).show();
            }
        });

    }

    private WeatherEntry getWeatherEntry(Settings settings) {
        WeatherEntry entry = settings.weatherEntry;
        if (entry.timestamp == -1L) {
            entry.setFakeData();
        }
        return entry;
    }

    private void setupPurchaseHint(Settings settings) {
        boolean purchasedWeatherData = purchased(BillingHelperActivity.ITEM_WEATHER_DATA);
        Log.i(TAG, "purchasedWeather:" + purchasedWeatherData);
        int layoutID = settings.getClockLayoutID(true);
        if (layoutID == ClockLayout.LAYOUT_ID_CALENDAR
                && !purchased(BillingHelperActivity.ITEM_DONATION)) {
            textViewPurchaseHint.setText(getContext().getString(R.string.gift_for_donors));
            textViewPurchaseHint.setVisibility(View.VISIBLE);

        } else if (layoutID >= ClockLayout.LAYOUT_ID_ANALOG2
                && !purchased(BillingHelperActivity.ITEM_WEATHER_DATA)) {
            textViewPurchaseHint.setText(getContext().getString(R.string.product_name_weather));
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
                        && layoutID != ClockLayout.LAYOUT_ID_ANIM_DIGITAL
        );
    }

    public enum PreviewMode {DAY, NIGHT}
}
