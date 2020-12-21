package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.text.DateFormat;
import java.util.Date;

public class WeatherPreviewPreference extends Preference {
    private View preferenceView = null;
    private TextView lastWeatherCalculationTime = null;
    private TextView lastLocationUpdateTime = null;
    private TextView cityName = null;

    public WeatherPreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WeatherPreviewPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
                layoutInflater.inflate(R.layout.weather_preview_preference, summaryParent2, true);

                lastWeatherCalculationTime = summaryParent2.findViewById(R.id.lastWeatherCalculationTime);
                lastLocationUpdateTime = summaryParent2.findViewById(R.id.lastLocationUpdateTime);
                cityName = summaryParent2.findViewById(R.id.lastCityName);
            }
        }

        updateView();
    }

    protected void updateView() {
        Settings settings = new Settings(getContext());
        WeatherEntry entry = settings.weatherEntry;
        lastLocationUpdateTime.setText(toDateTimeString(settings.location_time));
        lastWeatherCalculationTime.setText(toDateTimeString(entry.request_timestamp));
        if (entry.cityID != 0) {
            cityName.setText(entry.cityName + " (" + entry.cityID + ")");
        }
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
