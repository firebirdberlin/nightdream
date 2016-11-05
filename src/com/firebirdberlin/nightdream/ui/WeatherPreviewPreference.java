package com.firebirdberlin.nightdream.ui;

import java.util.Date;
import java.text.DateFormat;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.preference.Preference;
import android.util.AttributeSet;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.models.WeatherEntry;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.ui.ClockLayout;

public class WeatherPreviewPreference extends Preference {
    private View preferenceView = null;
    private Context context = null;
    private TextView lastWeatherCalculationTime = null;
    private TextView lastLocationUpdateTime = null;
    private TextView cityName = null;

    public WeatherPreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public WeatherPreviewPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
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
                layoutInflater.inflate(R.layout.weather_preview_preference, summaryParent2, true);

                lastWeatherCalculationTime = (TextView) summaryParent2.findViewById(R.id.lastWeatherCalculationTime);
                lastLocationUpdateTime = (TextView) summaryParent2.findViewById(R.id.lastLocationUpdateTime);
                cityName = (TextView) summaryParent2.findViewById(R.id.lastCityName);
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
        WeatherEntry entry = settings.weatherEntry;
        lastLocationUpdateTime.setText(toDateTimeString(settings.location_time));
        lastWeatherCalculationTime.setText(toDateTimeString(entry.timestamp * 1000));
        cityName.setText(entry.cityName + " (" + entry.cityID + ")");
    }

    public String toDateTimeString(long timestamp) {
        if ( timestamp > -1L ) {
            Date date = new Date(timestamp);
            DateFormat df = DateFormat.getDateTimeInstance();
            return df.format(date);
        }
        return "";
    }
}
