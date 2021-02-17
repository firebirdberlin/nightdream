package com.firebirdberlin.nightdream;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.text.LineBreaker;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.firebirdberlin.nightdream.models.FontCache;
import com.firebirdberlin.nightdream.ui.WeatherForecastLayout;
import com.firebirdberlin.nightdream.ui.WeatherLayout;
import com.firebirdberlin.openweathermapapi.ForecastRequestTask;
import com.firebirdberlin.openweathermapapi.ForcecastRequestTaskToday;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WeatherForecastTabWeather extends Fragment implements
        ForecastRequestTask.AsyncResponse,
        ForcecastRequestTaskToday.AsyncResponse{
    final static String TAG = "WeatherForeTabWeather";
    final int PERMISSIONS_REQUEST_ACCESS_LOCATION = 4000;
    private LinearLayout scrollViewLayout = null;
    private ScrollView scrollView = null;
    private Context context;
    private Settings settings;
    private int fadeDuration = 2000;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_weather_forecast_scrollview, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scrollViewLayout = view.findViewById(R.id.scroll_view_layout);
        scrollView = view.findViewById(R.id.scroll_view);
        context = getContext();
    }

    private void addWeatherEntries(List<WeatherEntry> entries) {
        Log.d(TAG,"addWeatherEntries");
        Log.i(TAG, String.format(" > got %d entries", entries.size()));

        if (context != null) {
            scrollViewLayout.removeAllViews();
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
                    dateView.setTextColor(getActivity().getResources().getColor(R.color.blue, null));
                } else {
                    dateView.setTextAppearance(context, android.R.style.TextAppearance_Medium);
                    dateView.setTextColor(getActivity().getResources().getColor(R.color.blue));
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

            // avoid flickering of the UI
            scrollView.animate().setDuration(fadeDuration).alpha(1);
            fadeDuration = 1000;
        }
    }

    private void actionBarSetup(String subtitle) {
        if ( ((AppCompatActivity)getActivity()) != null) {
            ActionBar ab = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (ab != null) {
                ab.setTitle(R.string.weather);
                ab.setSubtitle(subtitle);
            }
        }
    }

    private void addWeatherNow(WeatherEntry entry){
        Log.d(TAG,"addWeatherNow()");

        String timeFormat = settings.getFullTimeFormat();
        Log.d(TAG,"addWeatherNow() city: "+entry.cityName);

        if (getActivity() != null) {
            ConstraintLayout todayView = getActivity().findViewById(R.id.todayView);
            todayView.removeAllViews();

            WeatherForecastLayout layout = new WeatherForecastLayout(context);
            layout.setTimeFormat(timeFormat);
            layout.setTemperature(true, settings.temperatureUnit);
            layout.setWindSpeed(true, settings.speedUnit);
            layout.setSunrise(true, entry.sunriseTime);
            layout.setSunset(true, entry.sunsetTime);
            layout.update(entry);
            layout.findViewById(R.id.timeView).setVisibility(View.GONE);
            layout.findViewById(R.id.layoutClouds).setVisibility(View.GONE);
            layout.findViewById(R.id.humidityText).setVisibility(View.GONE);
            layout.findViewById(R.id.layoutRain).setVisibility(View.GONE);
            todayView.addView(layout);
        }
    }

    public void setupForecastPreview(Settings settings) {
        this.settings = settings;

        List<WeatherEntry> entries = getFakeEntries();

        if (scrollViewLayout != null) {
            scrollViewLayout.removeAllViews();
        }

        TextView textView = new TextView(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textView.setLayoutParams(layoutParams);
        int color = Utility.getRandomMaterialColor(context);
        int textColor = Utility.getContrastColor(color);
        textView.setGravity(Gravity.CENTER);
        textView.setBackgroundColor(color);
        textView.setTextColor(textColor);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        textView.setPadding(10, 10, 10,  10);
        textView.setText(getString(R.string.product_name_weather));

        Typeface typeface = FontCache.get(context, "fonts/dancingscript_regular.ttf");
        textView.setTypeface(typeface);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Drawable icon = ContextCompat.getDrawable(context, R.drawable.ic_googleplay);
            icon.setColorFilter(new PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC_ATOP));
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, icon, null);
        }
        View.OnClickListener purchaseListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((WeatherForecastActivity)getActivity()).showPurchaseDialog();
                Log.d(TAG,"onclick showPurchaseDialog");
            }
        };
        textView.setOnClickListener(purchaseListener);
        scrollViewLayout.addView(textView);

        TextView textView1 = new TextView(context);
        textView1.setLayoutParams(layoutParams);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textView1.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD);
        } else {
            textView1.setGravity(Gravity.CENTER);
        }
        int dp20 = Utility.dpToPx(context, 12);
        textView1.setPadding(dp20, dp20, dp20, dp20);
        textView1.setText(getString(R.string.product_description_weather));
        textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        scrollViewLayout.addView(textView1);

        TextView textView2 = new TextView(context);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams2.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
        textView2.setLayoutParams(layoutParams2);
        textView2.setBackgroundColor(color);
        textView2.setTextColor(textColor);
        textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        textView2.setPadding(10, dp20, 10, 0);
        textView2.setText(getString(R.string.upgrade_now));

        textView2.setTypeface(typeface);
        textView2.setOnClickListener(purchaseListener);
        scrollViewLayout.addView(textView2);

        WeatherLayout statusLine = new WeatherLayout(context);
        statusLine.setColor(Color.WHITE);
        statusLine.setGravity(Gravity.CENTER);
        statusLine.setWindSpeed(true, WeatherEntry.METERS_PER_SECOND);
        statusLine.update(entries.get(0));
        statusLine.setPadding(dp20, dp20, dp20, dp20);

        scrollViewLayout.addView(statusLine);
        addWeatherEntries(entries);
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

    @SuppressLint("MissingPermission")
    public void getWeatherForCurrentLocation(LocationManager locationManager, LocationListener locationListener, Settings settings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!((WeatherForecastActivity)getActivity()).hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSIONS_REQUEST_ACCESS_LOCATION
                );
                return;
            }
        }
        Log.i(TAG, "searching location");
        getLastKnownLocation(locationManager, locationListener, settings);
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 0, 0, locationListener
        );
    }

    private void getLastKnownLocation(LocationManager locationManager, LocationListener locationListener, Settings settings) {
        if (!((WeatherForecastActivity)getActivity()).hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return;
        }
        List<String> providers = locationManager.getProviders(true);
        for (String provider : providers) {
            if (LocationManager.GPS_PROVIDER.equals(provider)) {
                continue;
            }
            @SuppressLint("MissingPermission")
            Location location = locationManager.getLastKnownLocation(provider);
            if (location == null) continue;
            City city = new City();
            city.lat = location.getLatitude();
            city.lon = location.getLongitude();
            city.id = -1;
            city.name = "current";
            if (locationManager != null && locationListener != null) {
                locationManager.removeUpdates(locationListener);
            }
            requestWeather(city, settings);
        }
    }

    public void requestWeather(City city, Settings settings) {
        Log.i(TAG, "requestWeather()");

        this.settings = settings;
        if (city != null) {
            new ForecastRequestTask(this, settings.getWeatherProvider(), context).execute(city.toJson());
            new ForcecastRequestTaskToday(this, settings.getWeatherProvider(), context).execute(city.toJson());
        }
    }

    private void hide() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            // avoid flickering during build
            scrollView.setAlpha(0);
        }
    }

    @Override
    public void onRequestFinished(WeatherEntry entry) {
        Log.i(TAG, "onRequestFinished() WeatherEntry: "+entry);
        addWeatherNow(entry);
    }

    @Override
    public void onRequestFinished(List<WeatherEntry> entries) {
        Log.i(TAG, "onRequestFinished() List<WeatherEntry>");
        hide();

        if (entries.size() > 0) {
            WeatherEntry firstEntry = entries.get(0);
            actionBarSetup(firstEntry.cityName);
        }

        addWeatherEntries(entries);
    }

}
