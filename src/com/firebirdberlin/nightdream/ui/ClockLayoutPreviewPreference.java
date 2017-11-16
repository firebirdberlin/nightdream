package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

public class ClockLayoutPreviewPreference extends Preference {
    private static PreviewMode previewMode = PreviewMode.DAY;
    private ClockLayout clockLayout = null;
    private View preferenceView = null;
    private Context context = null;
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

    @Override
    protected View onCreateView(ViewGroup parent) {
        preferenceView = super.onCreateView(parent);

        View summary = preferenceView.findViewById(android.R.id.summary);
        if (summary != null) {
            ViewParent summaryParent = summary.getParent();
            if (summaryParent instanceof ViewGroup) {
                final LayoutInflater layoutInflater =
                    (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                ViewGroup summaryParent2 = (ViewGroup) summaryParent;
                layoutInflater.inflate(R.layout.clock_layout_preference, summaryParent2, true);

                clockLayout = (ClockLayout) summaryParent2.findViewById(R.id.clockLayout);
            }
        }

        return preferenceView;
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        updateView();
    }

    protected void updateView() {
        Settings settings = new Settings(getContext());
        clockLayout.setBackgroundColor(Color.TRANSPARENT);
        clockLayout.setLayout(settings.clockLayout);
        clockLayout.setTypeface(settings.typeface);
        clockLayout.setPrimaryColor(previewMode == PreviewMode.DAY ? settings.clockColor : settings.clockColorNight);
        clockLayout.setSecondaryColor(previewMode == PreviewMode.DAY ? settings.secondaryColor : settings.secondaryColorNight);

        clockLayout.setDateFormat(settings.dateFormat);
        clockLayout.setTimeFormat(settings.timeFormat12h, settings.timeFormat24h);
        clockLayout.showDate(settings.showDate);

        clockLayout.setTemperature(settings.showTemperature, settings.temperatureUnit);
        clockLayout.setWindSpeed(settings.showWindSpeed, settings.speedUnit);
        clockLayout.showWeather(settings.showWeather);

        WeatherEntry entry = getWeatherEntry(settings);
        clockLayout.update(entry);

        Utility utility = new Utility(getContext());
        Point size = utility.getDisplaySize();
        Configuration config = context.getResources().getConfiguration();
        clockLayout.updateLayout(size.x - preferenceView.getPaddingLeft()
                                        - preferenceView.getPaddingRight(),
                                 config);

        clockLayout.requestLayout();
    }

    private WeatherEntry getWeatherEntry(Settings settings) {
        WeatherEntry entry = settings.weatherEntry;
        if ( entry.timestamp ==  -1L) {
            entry.setFakeData();
        }
        return entry;
    }

    public enum PreviewMode {DAY, NIGHT}
}
