package com.firebirdberlin.nightdream.ui;


import java.lang.Math;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.models.WeatherEntry;

public class WeatherLayout extends LinearLayout {

    private Context context = null;
    private TextView iconText = null;
    private TextView iconWind = null;
    private TextView temperatureText = null;
    private TextView windText = null;
    private int temperatureUnit = WeatherEntry.CELSIUS;

    public WeatherLayout(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public WeatherLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater)
            context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View child = inflater.inflate(R.layout.weather_layout, null);
        addView(child);
    }

    public void setTemperatureUnit(int unit) {
        this.temperatureUnit = unit;
    }

    @Override
    protected void onFinishInflate() {
        iconText = (TextView) findViewById(R.id.iconText);
        iconWind = (TextView) findViewById(R.id.iconWind);
        temperatureText = (TextView) findViewById(R.id.temperatureText);
        windText = (TextView) findViewById(R.id.windText);
        Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/meteocons.ttf");
        iconText.setTypeface(typeface);
        iconWind.setTypeface(typeface);
    }

    public void clear() {
        iconText.setText("");
        iconWind.setText("");
        temperatureText.setText("");
        temperatureText.invalidate();
        windText.setText("");
        windText.invalidate();
        iconText.invalidate();
        iconWind.invalidate();
    }

    public void setTypeface(Typeface typeface) {
        temperatureText.setTypeface(typeface);
        windText.setTypeface(typeface);
    }

    public void setColor(int color) {
        if (iconText != null) {
            iconText.setTextColor(color);
        }
        if (iconWind != null) {
            iconWind.setTextColor(color);
        }
        if (temperatureText != null) {
            temperatureText.setTextColor(color);
        }
        if (windText != null) {
            windText.setTextColor(color);
        }
    }

    public void update(WeatherEntry entry) {
        if (iconText == null || temperatureText == null) return;
        long age = entry.ageMillis();
        if (entry.timestamp > -1L && age < 8 * 60 * 60 * 1000) {
            iconText.setText(iconToText(entry.weatherIcon));
            temperatureText.setText(formatTemperatureText(entry));
            iconWind.setText("F");
            windText.setText(formatWindText(entry));
        } else {
            iconText.setText("");
            iconWind.setText("");
            temperatureText.setText("");
            windText.setText("");
        }
        temperatureText.invalidate();
        windText.invalidate();
        iconText.invalidate();
        iconWind.invalidate();
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
        if (code.equals("50n") ) return "C";
        return "";
    }

    private String formatTemperatureText(WeatherEntry entry) {
        switch (temperatureUnit) {
            case WeatherEntry.CELSIUS:
                return String.format("%.1f°C", toDegreesCelcius(entry.temperature));
            case WeatherEntry.FAHRENHEIT:
                return String.format("%.1f°F", toDegreesFahrenheit(entry.temperature));
            default:
                return String.format("%.1fK", entry.temperature);
        }
    }
    private double toDegreesCelcius(double kelvin) {
        return kelvin - 273.15;
    }

    private double toDegreesFahrenheit(double kelvin) {
        return kelvin * 1.8 - 459.67;
    }

    private String formatWindText(WeatherEntry entry) {
        return String.format("%.1fm/s", entry.windSpeed);
    }
}
