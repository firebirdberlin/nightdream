package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.FontCache;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

public class WeatherLayout extends LinearLayout {
    private static final String TAG = "WeatherLayout";
    private static String NAMESPACE = "weather";
    private Context context;
    private DirectionIconView iconWindDirection = null;
    private TextView iconText = null;
    private TextView iconWind = null;
    private TextView temperatureText = null;
    private TextView windText = null;
    private LinearLayout container = null;
    private boolean showApparentTemperature = false;
    private WeatherEntry weatherEntry = null;
    private boolean showIcon = false;
    private boolean showWindSpeed = false;
    private boolean showTemperature = false;
    private int maxWidth = -1;
    private int minFontSizePx = -1;
    private int maxFontSizePx = -1;
    private int iconSizeFactor = 1;
    private int speedUnit = WeatherEntry.METERS_PER_SECOND;
    private int temperatureUnit = WeatherEntry.CELSIUS;
    private boolean isVertical = false;
    private int iconHeight = -1;
    private String content = "icon|temperature|wind";

    public WeatherLayout(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public WeatherLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        String content = getAttributeStringValue(attrs, NAMESPACE, "content", "icon|temperature|wind");
        String orientation = getAttributeStringValue(attrs, NAMESPACE, "orientation", "horizontal");
        this.content = content;
        showIcon = content.contains("icon");
        showWindSpeed = content.contains("wind");
        showTemperature = content.contains("temperature");
        isVertical = "vertical".equals(orientation);
        init();
    }

    @Override
    public void setOrientation(int orientation) {
        isVertical = orientation == LinearLayout.VERTICAL;
        setViewVisibility();
    }


    private static String getAttributeStringValue(
            AttributeSet attrs, String namespace, String name, String defaultValue
    ) {
        String value = attrs.getAttributeValue(namespace, name);
        if (value == null) value = defaultValue;

        return value;
    }

    public void setIconSizeFactor(int iconSizeFactor) {
        this.iconSizeFactor = iconSizeFactor;
    }

    public void setTemperature(boolean on, boolean showApparentTemperature, int unit) {
        on = content.contains("temperature") && on;
        this.showTemperature = on;
        this.showApparentTemperature = showApparentTemperature;
        this.temperatureUnit = unit;
        temperatureText.setVisibility((on) ? View.VISIBLE : View.GONE);
    }

    public void setWindSpeed(boolean on, int unit) {
        on = content.contains("wind") && on;
        this.showWindSpeed = on;
        this.speedUnit = unit;

        iconWind.setVisibility((on) ? View.VISIBLE : View.GONE);
        iconWindDirection.setVisibility((on) ? View.VISIBLE : View.GONE);
        windText.setVisibility((on) ? View.VISIBLE : View.GONE);
    }

    public void setIcon(boolean on) {
        on = content.contains("icon") && on;
        iconText.setVisibility((on) ? View.VISIBLE : View.GONE);
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child = inflater.inflate(R.layout.weather_layout, null);
        addView(child);

        container = findViewById(R.id.container);
        iconText = findViewById(R.id.iconText);
        iconWind = findViewById(R.id.iconWind);
        iconWindDirection = findViewById(R.id.iconWindDirection);
        temperatureText = findViewById(R.id.temperatureText);
        windText = findViewById(R.id.windText);

        Typeface typeface = FontCache.get(context, "fonts/meteocons.ttf");
        iconText.setTypeface(typeface);
        iconWind.setTypeface(typeface);

        setViewVisibility();
    }

    public void setMaxWidth(int width) {
        this.maxWidth = width;
    }

    void setViewVisibility() {
        container.setOrientation(isVertical ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        iconWind.setVisibility((showWindSpeed) ? View.VISIBLE : View.GONE);
        iconWindDirection.setVisibility((showWindSpeed) ? View.VISIBLE : View.GONE);
        windText.setVisibility((showWindSpeed) ? View.VISIBLE : View.GONE);

        temperatureText.setVisibility((showTemperature) ? View.VISIBLE : View.GONE);
        iconText.setVisibility((showIcon) ? View.VISIBLE : View.GONE);
    }

    public void clear() {
        iconText.setText("");
        iconWind.setText("");
        iconWindDirection.setDirection(DirectionIconView.INVALID);
        temperatureText.setText("");
        windText.setText("");

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

    public void setMaxFontSizesInSp(float minSize, float maxSize) {
        this.minFontSizePx = (int) Utility.spToPx(context, minSize);
        this.maxFontSizePx = (int) Utility.spToPx(context, maxSize);
    }

    public void setColor(int color) {
        if (iconText != null) {
            iconText.setTextColor(color);
        }
        if (iconWind != null) {
            iconWind.setTextColor(color);
        }
        if (iconWindDirection != null) {
            iconWindDirection.setColor(color);
        }
        if (temperatureText != null) {
            temperatureText.setTextColor(color);
        }
        if (windText != null) {
            windText.setTextColor(color);
        }
    }

    public void update(WeatherEntry entry) {
        this.weatherEntry = entry;
        if (iconText == null || temperatureText == null) return;
        long age = entry.ageMillis();
        if (entry.timestamp > -1L && age < 8 * 60 * 60 * 1000) {

            iconText.setText(iconToText(entry.weatherIcon));

            temperatureText.setText(entry.formatTemperatureText(temperatureUnit, showApparentTemperature));
            iconWind.setText("F");
            iconWindDirection.setDirection(entry.windDirection);
            windText.setText(entry.formatWindText(speedUnit));

            update();
        } else {
            clear();
        }
    }

    public void setTextSize(int unit, int size) {
        iconText.setTextSize(unit, iconSizeFactor * size);
        iconWind.setTextSize(unit, iconSizeFactor * size);
        windText.setTextSize(unit, size);
        temperatureText.setTextSize(unit, size);
        invalidate();
    }

    private String iconToText(String code) {
        // openweathermap
        if (code.equals("01d")) return "B";
        if (code.equals("01n")) return "C";
        if (code.equals("02d")) return "H";
        if (code.equals("02n")) return "I";
        if (code.equals("03d")) return "N";
        if (code.equals("03n")) return "N";
        if (code.equals("04d")) return "Y";
        if (code.equals("04n")) return "Y";
        if (code.equals("09d")) return "R";
        if (code.equals("09n")) return "R";
        if (code.equals("10d")) return "Q";
        if (code.equals("10n")) return "Q";
        if (code.equals("11d")) return "0";
        if (code.equals("11n")) return "0";
        if (code.equals("13d")) return "W";
        if (code.equals("13n")) return "W";
        if (code.equals("50d")) return "M";
        if (code.equals("50n")) return "M";
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

    public void update() {
        if (iconText == null || temperatureText == null) return;
        adjustTextSize();
        temperatureText.invalidate();
        if (this.showWindSpeed && weatherEntry != null) {
            iconWind.setVisibility((weatherEntry.windDirection >= 0) ? View.GONE : View.VISIBLE);
            iconWindDirection.setVisibility((weatherEntry.windDirection >= 0) ? View.VISIBLE : View.GONE);
        }
        windText.invalidate();
        iconText.invalidate();
        iconWind.invalidate();
        fixIconWindDirectionSize();
    }

    private void adjustTextSize() {
        if (maxWidth == -1) return;
        if (maxFontSizePx == -1 || minFontSizePx == -1) return;
        for (int size = minFontSizePx; size <= maxFontSizePx; size++) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
            if (measureText() > maxWidth) {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, size - 1);
                break;
            }
        }
    }

    public int getIconHeight() {
        return Utility.getHeightOfView(iconText);
    }

    public void setIconHeight(int height) {
        this.iconHeight = height;
    }

    private void fixIconWindDirectionSize() {
        int height = Utility.getHeightOfView(temperatureText);
        if (iconHeight > 0) height = iconHeight;
        LayoutParams layoutParams = (LayoutParams) iconWindDirection.getLayoutParams();
        layoutParams.width = height;
        layoutParams.height = height;
        iconWindDirection.setLayoutParams(layoutParams);
        iconWindDirection.requestLayout();
        iconWindDirection.invalidate();
    }

    public int measureText() {
        int textSize = 0;
        textSize += showIcon ? measureText(iconText) : 0;
        textSize += showTemperature ? measureText(temperatureText) : 0;
        if (showWindSpeed) {
            if (iconWindDirection != null) {
                // temperatureText is used to determine the line height
                textSize += measureText(temperatureText);
            } else {
                textSize += measureText(iconWind);
            }
            textSize += measureText(windText);
        }

        // add 10% for padding
        textSize += textSize / 10;
        return textSize;
    }

    private float measureText(TextView view) {
        String text = view.getText().toString();
        return view.getPaint().measureText(text);
    }
}
