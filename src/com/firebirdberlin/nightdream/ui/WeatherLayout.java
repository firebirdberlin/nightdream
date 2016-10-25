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
    private TextView temperatureText = null;

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

    @Override
    protected void onFinishInflate() {
        iconText = (TextView) findViewById(R.id.iconText);
        temperatureText = (TextView) findViewById(R.id.temperatureText);
        Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/meteocons.ttf");
        iconText.setTypeface(typeface);
    }

    public void clear() {
        iconText.setText("");
        temperatureText.setText("");
    }

    public void setTypeface(Typeface typeface) {
        if (iconText == null) return;
        temperatureText.setTypeface(typeface);
    }

    public void setColor(int color) {
        if (iconText != null) {
            iconText.setTextColor(color);
        }
        if (temperatureText != null) {
            temperatureText.setTextColor(color);
        }
    }

    public void update(WeatherEntry entry) {
        if (iconText == null) return;
        iconText.setText(iconToText(entry.weatherIcon));

        temperatureText.setText(String.valueOf(toDegreesCelcius(entry.temperature)) + "°C");
        // temperatureText.setText(String.valueOf((int) toFahrenheit(entry.temperature)) + "°F");
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

    private long toDegreesCelcius(double kelvin) {
        return Math.round(kelvin - 273.15);
    }
    private double toFahrenheit(double kelvin) {
        return kelvin * 1.8 - 459.67;
    }
}
