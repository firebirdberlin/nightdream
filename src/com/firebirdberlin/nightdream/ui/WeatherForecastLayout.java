package com.firebirdberlin.nightdream.ui;


import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class WeatherForecastLayout extends LinearLayout {

    private static final String TAG = "NightDream.WeatherForecastLayout";
    private Context context = null;
    private TextView timeView = null;
    private TextView iconText = null;
    private TextView iconWind = null;
    private String timeFormat = "HH:mm";
    private DirectionIconView iconWindDirection = null;
    private TextView temperatureText = null;
    private TextView windText = null;
    private int temperatureUnit = WeatherEntry.CELSIUS;
    private int speedUnit = WeatherEntry.METERS_PER_SECOND;
    private WeatherEntry weatherEntry = null;

    public WeatherForecastLayout(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public WeatherForecastLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater)
            context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View child = inflater.inflate(R.layout.weather_forecast_layout, null);
        addView(child);

        timeView = (TextView) findViewById(R.id.timeView);
        iconText = (TextView) findViewById(R.id.iconText);
        iconWind = (TextView) findViewById(R.id.iconWind);
        iconWindDirection = (DirectionIconView) findViewById(R.id.iconWindDirection);
        temperatureText = (TextView) findViewById(R.id.temperatureText);
        windText = (TextView) findViewById(R.id.windText);
        Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/meteocons.ttf");
        iconText.setTypeface(typeface);
        iconWind.setTypeface(typeface);
    }

    public void setTimeFormat(String format) {
        timeFormat = format;
    }

    public void setTemperature(boolean on, int unit) {
        this.temperatureUnit = unit;
        temperatureText.setVisibility( (on) ? View.VISIBLE : View.GONE );
    }

    public void setWindSpeed(boolean on, int unit) {
        this.speedUnit = unit;

        iconWind.setVisibility( (on) ? View.VISIBLE : View.GONE );
        iconWindDirection.setVisibility((on) ? View.VISIBLE : View.GONE);

        windText.setVisibility( (on) ? View.VISIBLE : View.GONE );
    }

    public void clear() {
        timeView.setText("");
        iconText.setText("");
        iconWind.setText("");
        iconWindDirection.setDirection(DirectionIconView.INVALID);
        temperatureText.setText("");
        windText.setText("");
        timeView.invalidate();
        iconText.invalidate();
        iconWind.invalidate();
        iconWindDirection.invalidate();
        temperatureText.invalidate();
        windText.invalidate();
    }

    public void setTypeface(Typeface typeface) {
        temperatureText.setTypeface(typeface);
        windText.setTypeface(typeface);
    }

    public void update(WeatherEntry entry) {
        Log.d("WeatherForecastLayout", entry.toString());
        Log.d("WeatherForecastLayout", formatTemperatureText(entry));
        this.weatherEntry = entry;
        if (iconText == null || temperatureText == null) return;
        iconWindDirection.setColor(Color.WHITE);
        Calendar mCalendar = Calendar.getInstance();
        mCalendar.setTimeInMillis(entry.timestamp * 1000);

        SimpleDateFormat sdf = new SimpleDateFormat(timeFormat);
        String text = sdf.format(mCalendar.getTime());
        timeView.setText(text);
        long age = entry.ageMillis();

        iconText.setText(iconToText(entry.weatherIcon));
        temperatureText.setText(formatTemperatureText(entry));
        iconWind.setText("F");
        iconWindDirection.setDirection(entry.windDirection);
        windText.setText(formatWindText(entry));

        if (weatherEntry != null) {
            iconWind.setVisibility((weatherEntry.windDirection >= 0) ? View.GONE : View.VISIBLE);
            iconWindDirection.setVisibility((weatherEntry.windDirection >= 0) ? View.VISIBLE : View.GONE);
        }
        fixIconWindDirectionSize();
    }

    private String iconToText(String code) {
        if (code.equals("01d") ) return "B";
        if (code.equals("01n") ) return "C";
        if (code.equals("02d") ) return "H";
        if (code.equals("02n") ) return "I";
        if (code.equals("03d") ) return "N";
        if (code.equals("03n") ) return "N";
        if (code.equals("04d") ) return "Y";
        if (code.equals("04n") ) return "Y";
        if (code.equals("09d") ) return "R";
        if (code.equals("09n") ) return "R";
        if (code.equals("10d") ) return "Q";
        if (code.equals("10n") ) return "Q";
        if (code.equals("11d") ) return "0";
        if (code.equals("11n") ) return "0";
        if (code.equals("13d") ) return "W";
        if (code.equals("13n") ) return "W";
        if (code.equals("50d") ) return "M";
        if (code.equals("50n") ) return "M";
        return "";
    }

    private String formatTemperatureText(WeatherEntry entry) {
        switch (temperatureUnit) {
            case WeatherEntry.CELSIUS:
                return String.format("%.0f°C", toDegreesCelcius(entry.temperature));
            case WeatherEntry.FAHRENHEIT:
                return String.format("%.0f°F", toDegreesFahrenheit(entry.temperature));
            default:
                return String.format("%.0f K", entry.temperature);
        }
    }
    private double toDegreesCelcius(double kelvin) {
        return kelvin - 273.15;
    }

    private double toDegreesFahrenheit(double kelvin) {
        return kelvin * 1.8 - 459.67;
    }

    private String formatWindText(WeatherEntry entry) {
        switch (speedUnit) {
            case WeatherEntry.MILES_PER_HOUR:
                return String.format("%.1f mi/h", toMilesPerHour(entry.windSpeed));
            case WeatherEntry.KM_PER_HOUR:
                return String.format("%.1f km/h", toKilometersPerHour(entry.windSpeed));
            case WeatherEntry.METERS_PER_SECOND:
            default:
                return String.format("%.1f m/s", entry.windSpeed);
        }
    }

    private double toMilesPerHour(double mps) {
        return mps  * 3600. / 1609.344;
    }

    private double toKilometersPerHour(double mps) {
        return mps  * 3.6;
    }

    private void fixIconWindDirectionSize() {
        temperatureText.post(new Runnable() {
            public void run() {
                int height = temperatureText.getHeight();
                iconWindDirection.setLayoutParams(new LinearLayout.LayoutParams(height, height));
                iconWindDirection.requestLayout();
                iconWindDirection.invalidate();
            }
        });
    }
}
