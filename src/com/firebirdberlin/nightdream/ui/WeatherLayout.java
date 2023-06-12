package com.firebirdberlin.nightdream.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.FontCache;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;

public class WeatherLayout extends LinearLayout {
    private static final String TAG = "WeatherLayout";
    private static final String NAMESPACE = "weather";
    private final HashSet<String> cycleItems = new HashSet<>();
    private final Context context;
    TimeReceiver timeReceiver;
    private int weatherIconMode = Settings.WEATHER_ICON_MODE_DEFAULT;
    private DirectionIconView iconWindDirection = null;
    private TextView iconText = null;
    private ImageView iconImage = null;
    private TextView iconWind = null;
    private TextView temperatureText = null;
    private TextView locationText = null;
    private TextView windText = null;
    private LinearLayout container = null;
    private boolean showApparentTemperature = false;
    private WeatherEntry weatherEntry = null;
    private boolean showIcon = false;
    private boolean showWindSpeed = false;
    private boolean showTemperature = false;
    private boolean showLocation = false;
    private boolean cycle = false;
    private boolean indicateEmptyState = false;
    private int maxWidth = -1;
    private int maxHeight = -1;
    private int minFontSizePx = -1;
    private int maxFontSizePx = -1;
    private int iconSizeFactor = 1;
    private int speedUnit = WeatherEntry.METERS_PER_SECOND;
    private int temperatureUnit = WeatherEntry.CELSIUS;
    private boolean isVertical = false;
    private int iconHeight = -1;
    private String content = "icon|temperature|wind";
    private boolean widget = false;

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
        cycle = attrs.getAttributeBooleanValue(NAMESPACE, "cycle", false);
        indicateEmptyState = attrs.getAttributeBooleanValue(NAMESPACE, "indicateEmpty", false);
        this.content = content;
        showIcon = content.contains("icon");
        showWindSpeed = content.contains("wind");
        showTemperature = content.contains("temperature");
        showLocation = content.contains("location");
        isVertical = "vertical".equals(orientation);
        cycleItems.add("temperature");
        init();
    }

    private static String getAttributeStringValue(
            AttributeSet attrs, String namespace, String name, String defaultValue
    ) {
        String value = attrs.getAttributeValue(namespace, name);
        if (value == null) value = defaultValue;

        return value;
    }

    public void setWeatherIconMode(int weatherIconMode) {
        this.weatherIconMode = weatherIconMode;
    }

    @Override
    public void setOrientation(int orientation) {
        isVertical = orientation == LinearLayout.VERTICAL;
        setViewVisibility();
    }

    public void setIconSizeFactor(int iconSizeFactor) {
        this.iconSizeFactor = iconSizeFactor;
    }

    public void setTemperature(boolean on, boolean showApparentTemperature, int unit) {
        on = content.contains("temperature") && on;
        this.showTemperature = on;
        this.showApparentTemperature = showApparentTemperature;
        this.temperatureUnit = unit;
        if (on) {
            cycleItems.add("temperature");
        } else {
            cycleItems.remove("temperature");
        }
    }

    public void setWindSpeed(boolean on, int unit) {
        on = content.contains("wind") && on;
        this.showWindSpeed = on;
        this.speedUnit = unit;
        if (on) {
            cycleItems.add("wind");
        } else {
            cycleItems.remove("wind");
        }
    }

    public void setIcon(boolean on) {
        showIcon = content.contains("icon") && on;
    }

    public void setLocation(boolean on) {
        on = content.contains("location") && on;
        showLocation = on;
        locationText.setVisibility((on) ? View.VISIBLE : View.GONE);
        if (on) {
            cycleItems.add("location");
        } else {
            cycleItems.remove("location");
        }
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child = inflater.inflate(R.layout.weather_layout, null);
        addView(child);

        container = findViewById(R.id.container);
        iconText = findViewById(R.id.iconText);
        iconImage = findViewById(R.id.iconImage);
        iconWind = findViewById(R.id.iconWind);
        iconWindDirection = findViewById(R.id.iconWindDirection);
        temperatureText = findViewById(R.id.temperatureText);
        locationText = findViewById(R.id.locationText);
        windText = findViewById(R.id.windText);

        Typeface typeface = FontCache.get(context, "fonts/meteocons.ttf");
        iconText.setTypeface(typeface);
        iconWind.setTypeface(typeface);

        setViewVisibility();
    }

    public void setMaxWidth(int width) {
        this.maxWidth = width;
    }

    public void setMaxHeight(int height) {
        this.maxHeight = height;
    }

    public void setViewVisibility() {
        container.setOrientation(isVertical ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        if (cycle) {
            ArrayList<String> items = new ArrayList<>(cycleItems);
            Calendar cal = Calendar.getInstance();
            int itemId = cal.get(Calendar.MINUTE) % items.size();
            String item = items.get(itemId);

            boolean on = "temperature".equals(item);

            if (weatherIconMode == Settings.WEATHER_ICON_MODE_DEFAULT) {
                iconText.setVisibility(on ? View.VISIBLE : View.GONE);
                iconImage.setVisibility(View.GONE);
            } else {
                iconImage.setVisibility(on ? View.VISIBLE : View.GONE);
                iconText.setVisibility(View.GONE);
            }

            temperatureText.setVisibility(on ? View.VISIBLE : View.GONE);

            on = "wind".equals(item);
            if (weatherEntry != null) {
                iconWind.setVisibility((!on || weatherEntry.windDirection >= 0) ? View.GONE : View.VISIBLE);
                iconWindDirection.setVisibility((on && weatherEntry.windDirection >= 0) ? View.VISIBLE : View.GONE);
                windText.setVisibility(on ? View.VISIBLE : View.GONE);
            }

            on = "location".equals(item);
            locationText.setVisibility(on ? View.VISIBLE : View.GONE);
        } else {
            if (weatherIconMode == Settings.WEATHER_ICON_MODE_DEFAULT) {
                iconText.setVisibility(showIcon ? View.VISIBLE : View.GONE);
                iconImage.setVisibility(View.GONE);
            } else {
                iconImage.setVisibility(showIcon ? View.VISIBLE : View.GONE);
                iconText.setVisibility(View.GONE);
            }

            if (weatherEntry != null) {
                iconWind.setVisibility((!showWindSpeed || weatherEntry.windDirection >= 0) ? View.GONE : View.VISIBLE);
                iconWindDirection.setVisibility((showWindSpeed && weatherEntry.windDirection >= 0) ? View.VISIBLE : View.GONE);
                windText.setVisibility(showWindSpeed ? View.VISIBLE : View.GONE);
            }

            temperatureText.setVisibility((showTemperature) ? View.VISIBLE : View.GONE);

            locationText.setVisibility((showLocation) ? View.VISIBLE : View.GONE);
        }
    }

    public void clear() {
        iconText.setText("");
        iconImage.setImageDrawable(null);
        iconWind.setText("");
        iconWindDirection.setVisibility(VISIBLE);
        iconWindDirection.setDirection(DirectionIconView.INVALID);
        iconWindDirection.setVisibility(INVISIBLE);
        temperatureText.setText(indicateEmptyState ? "..." : "");
        locationText.setText(indicateEmptyState ? "..." : "");
        windText.setText(indicateEmptyState ? "..." : "");

        iconText.invalidate();
        iconImage.invalidate();
        iconWind.invalidate();
        iconWindDirection.invalidate();
        locationText.invalidate();
        temperatureText.invalidate();
        windText.invalidate();
    }

    public void setTypeface(Typeface typeface) {
        temperatureText.setTypeface(typeface);
        windText.setTypeface(typeface);
        locationText.setTypeface(typeface);
    }

    public void setMaxFontSizesInSp(float minSize, float maxSize) {
        this.minFontSizePx = Utility.spToPx(context, minSize);
        this.maxFontSizePx = Utility.spToPx(context, maxSize);
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
        if (locationText != null) {
            locationText.setTextColor(color);
        }
        if (windText != null) {
            windText.setTextColor(color);
        }
    }

    public void update(WeatherEntry entry) {
        Log.d(TAG, "update(WeatherEntry)");

        this.weatherEntry = entry;
        if (iconText == null || temperatureText == null || iconImage == null) return;
        if (shallBeVisible()) {
            iconText.setText(entry.weatherIconMeteoconsSymbol);

            if (weatherIconMode != Settings.WEATHER_ICON_MODE_DEFAULT) {
                Drawable d = getIconImage(entry);
                iconImage.setImageDrawable(d);

                if (weatherIconMode == Settings.WEATHER_ICON_MODE_ANIMATED
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && d instanceof AnimatedVectorDrawable) {
                    ((AnimatedVectorDrawable) d).start();
                }
            }

            temperatureText.setText(entry.formatTemperatureText(temperatureUnit, showApparentTemperature));
            locationText.setText(entry.cityName);
            iconWind.setText("F");
            iconWindDirection.setDirection(entry.windDirection);
            windText.setText(entry.formatWindText(speedUnit));
            update();
        } else {
            clear();
        }
    }

    public Drawable getIconImage(WeatherEntry entry) {
        String identifier = entry.getWeatherIconIdentifier(weatherIconMode, widget);

        int imageID = getResources().getIdentifier(
                identifier, "drawable", context.getPackageName()
        );
        if (imageID > 0) {
            return ContextCompat.getDrawable(context, imageID);
        } else {
            return null;
        }
    }

    public void setTextSizePx(int size) {
        int unit = TypedValue.COMPLEX_UNIT_PX;

        iconText.setTextSize(unit, iconSizeFactor * size);
        iconWind.setTextSize(unit, iconSizeFactor * size);
        windText.setTextSize(unit, size);
        temperatureText.setTextSize(unit, size);
        locationText.setTextSize(unit, size);
        iconImage.getLayoutParams().height = temperatureText.getHeight();
        iconImage.getLayoutParams().width = temperatureText.getHeight();

        iconImage.getLayoutParams().height = getIconHeight();
        iconImage.getLayoutParams().width = getIconHeight();

        invalidate();
    }

    public float getTextSize() {
        return temperatureText.getTextSize();
    }

    public void update() {
        setViewVisibility();
        adjustTextSize();
        temperatureText.invalidate();
        locationText.invalidate();
        windText.invalidate();
        iconText.invalidate();
        iconImage.invalidate();
        iconWind.invalidate();
        fixIconWindDirectionSize();
    }

    private void adjustTextSize() {
        if (maxWidth == -1) return;
        if (maxFontSizePx == -1 || minFontSizePx == -1) return;
        for (int size = minFontSizePx; size <= maxFontSizePx; size++) {
            setTextSizePx(size);
            if (measureText() > maxWidth || (maxHeight > 0 && measureTextHeight() > maxHeight)) {
                setTextSizePx(size - 1);
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
        textSize += isVisible(iconText) ? measureText(iconText) : 0;
        textSize += isVisible(temperatureText) ? measureText(temperatureText) : 0;
        textSize += isVisible(locationText) ? measureText(locationText) : 0;
        textSize += isVisible(windText) ? measureText(windText) : 0;
        textSize += isVisible(iconWind) ? measureText(iconWind) : 0;

        textSize += isVisible(iconWindDirection) ? measureText(temperatureText) : 0;
        // temperatureText is used to determine the line height

        // add 10% for padding
        textSize += textSize / 10;
        return textSize;
    }

    private boolean isVisible(View view) {
        return view != null && view.getVisibility() == VISIBLE;
    }

    public boolean shallBeVisible() {
        if (weatherEntry == null) return false;
        return weatherEntry.isValid();
    }

    private float measureText(TextView view) {
        String text = view.getText().toString();
        return view.getPaint().measureText(text);
    }

    public int measureTextHeight() {
        return Utility.getHeightOfView(this);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setTimeTick();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unsetTimeTick();
    }

    void setTimeTick() {
        timeReceiver = new TimeReceiver();
        context.registerReceiver(timeReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    void unsetTimeTick() {
        if (timeReceiver != null) {
            try {
                context.unregisterReceiver(timeReceiver);
            } catch (IllegalArgumentException e) {
                // receiver was not registered,
            }
            timeReceiver = null;
        }
    }

    public void setWidget(boolean widget) {
        this.widget = widget;
    }

    class TimeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {
            if (!cycle) unsetTimeTick();
            update();
        }
    }
}
