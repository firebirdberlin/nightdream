package com.firebirdberlin.nightdream;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.firebirdberlin.nightdream.ui.WeatherForecastLayout;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WeatherForecastTabPreview extends Fragment {
    private Context context;
    private ScrollView scrollView = null;
    private LinearLayout scrollViewLayout = null;
    private Settings settings;
    final static String TAG = "WeatherForeTabPreview";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_weather_forecast_preview, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View.OnClickListener purchaseListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((WeatherForecastActivity) getActivity()).showPurchaseDialog();
                Log.d(TAG, "onclick showPurchaseDialog");
            }
        };

        int color = Utility.getRandomMaterialColor(context);
        int textColor = Utility.getContrastColor(color);

        TextView title = view.findViewById(R.id.title);
        title.setTextColor(textColor);
        title.setBackgroundColor(color);
        title.setOnClickListener(purchaseListener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Drawable icon = ContextCompat.getDrawable(context, R.drawable.ic_googleplay);
            if (icon != null) {
                icon.setColorFilter(new PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC_ATOP));
                title.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, icon, null);
            }
        }

        TextView upgradeNow = view.findViewById(R.id.upgrade_now);
        upgradeNow.setOnClickListener(purchaseListener);
        upgradeNow.setBackgroundColor(color);
        upgradeNow.setTextColor(textColor);

        this.scrollView = view.findViewById(R.id.scroll_view);
        this.scrollViewLayout = view.findViewById(R.id.scroll_view_layout);
    }

    public void setupForecastPreview(Settings settings) {
        this.settings = settings;
        addWeatherEntries(getFakeEntries());
    }

    List<WeatherEntry> getFakeEntries() {
        List<WeatherEntry> entries = new ArrayList<>();

        Calendar now = Calendar.getInstance();
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        int hour = now.get(Calendar.HOUR);
        int diff = ((23 + 1) / 3 * 3 + 2) - hour;
        now.add(Calendar.HOUR, diff);
        WeatherEntry entry1 = new WeatherEntry();
        entry1.apparentTemperature = 292.15;
        entry1.cityName = "Bullerby";
        entry1.clouds = 71;
        entry1.humidity = 53;
        entry1.temperature = 293.15;
        entry1.timestamp = now.getTimeInMillis() / 1000;
        entry1.request_timestamp = System.currentTimeMillis();
        entry1.weatherIcon = "04n";
        entry1.windDirection = 247;
        entry1.windSpeed = 3.4;
        entries.add(entry1);
        WeatherEntry entry2 = new WeatherEntry();
        now.add(Calendar.HOUR, 3);
        entry2.apparentTemperature = 294.15;
        entry2.clouds = 57;
        entry2.humidity = 57;
        entry2.rain3h = 1.3;
        entry2.temperature = 295.15;
        entry2.timestamp = now.getTimeInMillis() / 1000;
        entry1.request_timestamp = System.currentTimeMillis();
        entry2.weatherIcon = "03n";
        entry2.windDirection = 266;
        entry2.windSpeed = 2.3;
        entries.add(entry2);
        return entries;
    }

    private void addWeatherEntries(List<WeatherEntry> entries) {
        Log.d(TAG, "addWeatherEntries");
        Log.d(TAG, String.format(" > got %d entries", entries.size()));

        if (context != null) {
            String timeFormat = settings.getFullTimeFormat();

            int day = -1;
            long now = System.currentTimeMillis();
            for (WeatherEntry entry : entries) {
                if (entry.timestamp * 1000 < now - 600000) {
                    continue;
                }
                TextView dateView = new TextView(context);
                if (Build.VERSION.SDK_INT >= 23) {
                    dateView.setTextAppearance(android.R.style.TextAppearance_Medium);
                    if (getActivity() != null) {
                        dateView.setTextColor(getActivity().getResources().getColor(R.color.blue, null));
                    }
                } else {
                    dateView.setTextAppearance(context, android.R.style.TextAppearance_Medium);
                    if (getActivity() != null) {
                        dateView.setTextColor(getActivity().getResources().getColor(R.color.blue));
                    }
                }
                dateView.setTypeface(null, Typeface.BOLD);

                Calendar mCalendar = Calendar.getInstance();
                mCalendar.setTimeInMillis(entry.timestamp * 1000);
                if (day != mCalendar.get(Calendar.DAY_OF_MONTH)) {
                    day = mCalendar.get(Calendar.DAY_OF_MONTH);
                    DateFormat sdf = DateFormat.getDateInstance(DateFormat.FULL);
                    String text = sdf.format(mCalendar.getTime());
                    dateView.setText(text);
                    scrollViewLayout.addView(dateView);
                }

                WeatherForecastLayout layout = new WeatherForecastLayout(context);
                layout.setTimeFormat(timeFormat);
                layout.setTemperature(true, settings.temperatureUnit);
                layout.setWindSpeed(true, settings.speedUnit);
                layout.update(entry);
                scrollViewLayout.addView(layout);
            }
        }
    }

}
