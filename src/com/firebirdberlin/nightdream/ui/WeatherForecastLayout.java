package com.firebirdberlin.nightdream.ui;


import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.WindSpeedConversion;
import com.firebirdberlin.nightdream.models.FontCache;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class WeatherForecastLayout extends LinearLayout {

    private static final String TAG = "WeatherForecastLayout";
    private Context context = null;
    private TextView timeView = null;
    private TextView iconClouds = null;
    private TextView iconText = null;
    private TextView iconWind = null;
    private TextView iconRain3h = null;
    private String timeFormat = "HH:mm";
    private DirectionIconView iconWindDirection = null;
    private TextView cloudText = null;
    private TextView temperatureText = null;
    private TextView descriptionText = null;
    private TextView humidityText = null;
    private TextView windText = null;
    private TextView rainText = null;
    private TextView sunSetTime = null;
    private TextView sunRiseTime = null;
    private LinearLayout sunSetLayout = null;
    private LinearLayout sunRiseLayout = null;
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
        Log.d(TAG,"init()");
        LayoutInflater inflater = (LayoutInflater)
            context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View child = inflater.inflate(R.layout.weather_forecast_layout, null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                       );
        addView(child, lp);

        timeView = findViewById(R.id.timeView);
        iconText = findViewById(R.id.iconText);
        iconWind = findViewById(R.id.iconWind);
        iconRain3h = findViewById(R.id.iconRain3h);
        iconClouds = findViewById(R.id.iconClouds);
        iconWindDirection = findViewById(R.id.iconWindDirection);
        cloudText = findViewById(R.id.cloudText);
        temperatureText = findViewById(R.id.temperatureText);
        descriptionText = findViewById(R.id.descriptionText);
        humidityText = findViewById(R.id.humidityText);
        rainText = findViewById(R.id.rainText);
        windText = findViewById(R.id.windText);
        sunRiseTime = findViewById(R.id.sunRiseTime);
        sunSetTime = findViewById(R.id.sunSetTime);
        sunRiseLayout = findViewById(R.id.layoutSunRise);
        sunSetLayout = findViewById(R.id.layoutSunSet);
        Typeface typeface = FontCache.get(context, "fonts/meteocons.ttf");
        iconClouds.setTypeface(typeface);
        iconText.setTypeface(typeface);
        iconWind.setTypeface(typeface);
        iconRain3h.setTypeface(typeface);
    }

    public void setTimeFormat(String format) {
        timeFormat = format;
    }

    public void setTemperature(boolean on, int unit) {
        this.temperatureUnit = unit;
        temperatureText.setVisibility( (on) ? View.VISIBLE : View.GONE );
    }

    public void setDescriptionText(boolean on, String value) {
        descriptionText.setText(value);
        descriptionText.setVisibility( (on) ? View.VISIBLE : View.GONE );
    }

    public void setSunrise(boolean on, Date time, boolean is24Hour) {
        String text = "-";

        if (time != null) {
            SimpleDateFormat sdf;
            if (is24Hour) {
                 sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            }
            else {
                 sdf = new SimpleDateFormat("hh:mm aa", Locale.getDefault());
            }
            text = sdf.format(time);
        }
        sunRiseTime.setText(text);
        sunRiseLayout.setVisibility( (on) ? View.VISIBLE : View.GONE );
    }

    public void setSunrise(boolean on, long time) {
        Calendar mCalendar = Calendar.getInstance();
        mCalendar.setTimeInMillis(time * 1000);

        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat(timeFormat, Locale.getDefault());
        String text = sdf.format(mCalendar.getTime());
        sunRiseTime.setText(text);
        sunRiseLayout.setVisibility( (on) ? View.VISIBLE : View.GONE );
    }

    public void setSunset(boolean on, Date time, boolean is24Hour) {
        String text = "-";
        if (time != null){
            SimpleDateFormat sdf;
            if (is24Hour) {
                sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            }
            else {
                sdf = new SimpleDateFormat("hh:mm aa", Locale.getDefault());
            }
            text = sdf.format(time);
        }

        sunSetTime.setText(text);
        sunSetLayout.setVisibility( (on) ? View.VISIBLE : View.GONE );
    }

    public void setSunset(boolean on, long time) {
        Calendar mCalendar = Calendar.getInstance();
        mCalendar.setTimeInMillis(time * 1000);

        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat(timeFormat, Locale.getDefault());

        String text = sdf.format(mCalendar.getTime());
        sunSetTime.setText(text);
        sunSetLayout.setVisibility( (on) ? View.VISIBLE : View.GONE );
    }

    public void setWindSpeed(boolean on, int unit) {
        this.speedUnit = unit;

        iconWind.setVisibility( (on) ? View.VISIBLE : View.GONE );
        iconWindDirection.setVisibility((on) ? View.VISIBLE : View.GONE);

        windText.setVisibility( (on) ? View.VISIBLE : View.GONE );
    }

    public void clear() {
        timeView.setText("");
        iconClouds.setText("");
        iconRain3h.setText("");
        iconText.setText("");
        iconWind.setText("");
        iconWindDirection.setDirection(DirectionIconView.INVALID);
        cloudText.setText("");
        temperatureText.setText("");
        humidityText.setText("");
        rainText.setText("");
        windText.setText("");
        timeView.invalidate();
        iconText.invalidate();
        iconWind.invalidate();
        iconWindDirection.invalidate();
        temperatureText.invalidate();
        humidityText.invalidate();
        windText.invalidate();
    }

    public void setTypeface(Typeface typeface) {
        temperatureText.setTypeface(typeface);
        humidityText.setTypeface(typeface);
        windText.setTypeface(typeface);
        rainText.setTypeface(typeface);
    }

    public void update(WeatherEntry entry) {
       // Log.d("WeatherForecastLayout", entry.toString());
       // Log.d("WeatherForecastLayout", entry.formatTemperatureText(temperatureUnit));
        this.weatherEntry = entry;
        if (iconText == null || temperatureText == null) return;
        iconWindDirection.setColor(Color.WHITE);
        Calendar mCalendar = Calendar.getInstance();
        mCalendar.setTimeInMillis(entry.timestamp * 1000);

        SimpleDateFormat sdf = new SimpleDateFormat(timeFormat,java.util.Locale.getDefault());
        String text = sdf.format(mCalendar.getTime());
        timeView.setText(text);

        iconText.setText(iconToText(entry.weatherIcon));
        temperatureText.setText(entry.formatTemperatureText(temperatureUnit));
        if (!entry.formatHumidityText().isEmpty()) {
            humidityText.setText(entry.formatHumidityText());
        }
        else
        {
            humidityText.setVisibility(View.GONE);
        }
        iconWind.setText("F");

        iconWindDirection.setDirection(entry.windDirection);
        windText.setText(formatWindText(entry));

        if (entry != null && entry.rain3h >= 0.) {
            iconRain3h.setText("R");
            rainText.setText(formatRainText(entry.rain3h));
        } else
        if (entry != null && entry.rain1h > 0.) {
            iconRain3h.setText("R");
            rainText.setText(formatRainText(entry.rain1h));
        } else {
            iconRain3h.setText("");
            iconRain3h.setVisibility(View.GONE);
            rainText.setText("");
            rainText.setVisibility(View.GONE);
        }

        if (entry != null && entry.clouds >= 0) {
            iconClouds.setText("Y");
            cloudText.setText(formatCloudsText(entry));
        } else {
            iconClouds.setText("");
            cloudText.setText("");
        }

        if (weatherEntry != null) {
            iconWind.setVisibility((weatherEntry.windDirection >= 0) ? View.GONE : View.VISIBLE);
            iconWindDirection.setVisibility((weatherEntry.windDirection >= 0) ? View.VISIBLE : View.GONE);
        }
        fixIconWindDirectionSize();
    }

    private String iconToText(String code) {
        // openweathermap
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
        // darksky
        if (code.equals("clear-day")) return "B";
        if (code.equals("clear-night")) return "C";
        if (code.equals("rain")) return "R";
        if (code.equals("snow")) return "W";
        if (code.equals("sleet")) return "X";
        if (code.equals("wind")) return "F";
        if (code.equals("fog")) return "M";
        if (code.equals("cloudy")) return "N";
        if (code.equals("partly-cloudy-day")) return "H";
        if (code.equals("partly-cloudy-night")) return "I";
        if (code.equals("thunderstorm")) return "0";
        if (code.equals("tornado")) return "0";
        if (code.equals("hail")) return "X";
        return "";
    }

    private String formatRainText(double rainValue) {
        return String.format(Locale.getDefault(),"%.1f mm", rainValue);
    }

    private String formatCloudsText(WeatherEntry entry) {
        return String.format(Locale.getDefault(), "%3d %%", entry.clouds);
    }

    private String formatWindText(WeatherEntry entry) {
        String windSpeedBeaufort = String.format(Locale.getDefault(), "%d Bft", WindSpeedConversion.metersPerSecondToBeaufort(entry.windSpeed));
        String formatted = entry.formatWindText(speedUnit);
        switch (speedUnit) {
            case WeatherEntry.MILES_PER_HOUR:
            case WeatherEntry.KM_PER_HOUR:
            case WeatherEntry.KNOT:
                return String.format("%s (%s)", formatted, windSpeedBeaufort);
            case WeatherEntry.BEAUFORT:
            case WeatherEntry.METERS_PER_SECOND:
            default:
                return String.format(Locale.getDefault(), "%.1f m/s (%s)", entry.windSpeed, windSpeedBeaufort);
        }
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
