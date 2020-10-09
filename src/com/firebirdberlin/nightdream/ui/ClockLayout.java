package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.CustomAnalogClock;
import com.firebirdberlin.nightdream.CustomDigitalClock;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.mNotificationListener;
import com.firebirdberlin.nightdream.models.AnalogClockConfig;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;
import com.google.android.flexbox.FlexboxLayout;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.util.Calendar;

public class ClockLayout extends LinearLayout {
    public static final int LAYOUT_ID_DIGITAL = 0;
    public static final int LAYOUT_ID_ANALOG = 1;
    public static final int LAYOUT_ID_ANALOG2 = 2;
    public static final int LAYOUT_ID_ANALOG3 = 3;
    public static final int LAYOUT_ID_ANALOG4 = 4;
    public static final int LAYOUT_ID_DIGITAL_FLIP = 5;
    public static final int LAYOUT_ID_CALENDAR = 6;
    public static final int LAYOUT_ID_DIGITAL2 = 7;
    private static final String TAG = "NightDream.ClockLayout";
    private int layoutId = LAYOUT_ID_DIGITAL;

    private Context context;
    private AutoAdjustTextView clock = null;
    private MaterialCalendarView calendarView = null;
    private AutoAdjustTextView clock_ampm = null;
    private CustomAnalogClock analog_clock = null;
    private AutoAdjustTextView date = null;
    private WeatherLayout weatherLayout = null;
    private WeatherLayout weatherLayout2 = null;
    private FlexboxLayout notificationLayout = null;
    private LinearLayout mediaStyleLayout = null;
    private View divider = null;
    private boolean showDivider = true;
    private boolean mirrorText = false;
    private boolean showNotifications = true;
    private int weatherIconSizeFactor = 3;

    public ClockLayout(Context context) {
        super(context);
        this.context = context;
    }

    public ClockLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    private void init() {
        if (getChildCount() > 0) {
            removeAllViews();
        }

        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child;
        if (layoutId == LAYOUT_ID_DIGITAL) {
            child = inflater.inflate(R.layout.clock_layout, null);
        } else if (layoutId == LAYOUT_ID_DIGITAL2) {
            child = inflater.inflate(R.layout.clock_layout_digital, null);
        } else if (layoutId == LAYOUT_ID_DIGITAL_FLIP) {
            child = inflater.inflate(R.layout.clock_layout_digital_flip, null);
        } else if (layoutId == LAYOUT_ID_CALENDAR) {
            child = inflater.inflate(R.layout.clock_layout_calendar, null);
        } else if (layoutId == LAYOUT_ID_ANALOG) {
            child = inflater.inflate(R.layout.analog_clock_layout, null);
        } else {
            child = inflater.inflate(R.layout.analog_clock_layout_4, null);
        }
        if (child != null) {
            addView(child);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        //return true;
        return false;
    }

    public void setLayout(int layoutId) {
        this.layoutId = layoutId;
        init();
        onFinishInflate();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Log.v(TAG, "onFinishInflate");
        calendarView = findViewById(R.id.calendarView);
        clock = findViewById(R.id.clock);
        clock_ampm = findViewById(R.id.clock_ampm);
        date = findViewById(R.id.date);
        weatherLayout = findViewById(R.id.weatherLayout);
        weatherLayout2 = findViewById(R.id.weatherLayout2);
        divider = findViewById(R.id.divider);
        analog_clock = findViewById(R.id.analog_clock);
        notificationLayout = findViewById(R.id.notificationbar);
        mediaStyleLayout = findViewById(R.id.notification_mediacontrol_bar);

        if (calendarView != null) {
            calendarView.setTopbarVisible(true);
            calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_SINGLE);
            calendarView.setLeftArrowMask(null);
            calendarView.setRightArrowMask(null);
            calendarView.setDynamicHeightEnabled(true);
        }
    }

    public void setWeatherIconSizeFactor(int weatherIconSizeFactor) {
        this.weatherIconSizeFactor = weatherIconSizeFactor;
    }

    public void setTypeface(Typeface typeface) {
        if (clock != null) {
            clock.setTypeface(typeface);

        }
        if (clock_ampm != null) {
            clock_ampm.setTypeface(typeface);
        }
    }

    public void setPrimaryColor(int color, int glowRadius, int glowColor, int textureId) {
        if (clock != null) {
            clock.setTextColor(color);
            applyTexture(clock, glowRadius, glowColor, textureId);
        }
        if (clock_ampm != null) {
            clock_ampm.setTextColor(color);
            applyTexture(clock_ampm, glowRadius, glowColor, textureId);
        }
        if (analog_clock != null) {
            analog_clock.setPrimaryColor(color);
        }

        if (layoutId == LAYOUT_ID_DIGITAL_FLIP) {
            CustomDigitalFlipClock layout = findViewById(R.id.time_layout);
            layout.setPrimaryColor(color);
        }
        if (calendarView != null) {
            calendarView.setSelectionColor(color);
        }
    }

    void applyTexture(TextView view, int glowRadius, int glowColor, int resId) {
        if (view == null) {
            return;
        }
        try {
            view.setShadowLayer(glowRadius, 0, 0, glowColor);
            if (resId > 0) {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
                BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
                view.getPaint().setShader(shader);
            }
        } catch (OutOfMemoryError e) {
            view.getPaint().setShader(null);
        }
        //view.setLayerType((glowRadius > 24) ? LAYER_TYPE_SOFTWARE : LAYER_TYPE_HARDWARE, null);
        view.setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setSecondaryColor(int color) {
        if (date != null) {
            date.setTextColor(color);
        }
        if (weatherLayout != null) {
            weatherLayout.setColor(color);
        }
        if (weatherLayout2 != null) {
            weatherLayout2.setColor(color);
        }
        if (divider != null) {
            divider.setBackgroundColor(color);
        }
        if (analog_clock != null) {
            analog_clock.setSecondaryColor(color);
        }

        if (layoutId == LAYOUT_ID_DIGITAL_FLIP) {
            CustomDigitalFlipClock layout = findViewById(R.id.time_layout);
            layout.setSecondaryColor(color);
        }
        Utility.colorizeView(calendarView, color, PorterDuff.Mode.MULTIPLY);
        Utility.colorizeView(notificationLayout, color);
    }

    public void setTemperature(boolean on, boolean withApparentTemperature, int unit) {
        if (weatherLayout == null) return;
        weatherLayout.setTemperature(on, withApparentTemperature, unit);
    }

    public void setWindSpeed(boolean on, int unit) {
        if (layoutId == LAYOUT_ID_DIGITAL2 && weatherLayout2 != null) {
            weatherLayout2.setWindSpeed(on, unit);
        } else {
            weatherLayout.setWindSpeed(on, unit);
        }
    }

    public void setShowDivider(boolean on) {
        this.showDivider = on;
    }

    public void setMirrorText(boolean on) {
        this.mirrorText = on;
    }

    public void showDate(boolean on) {
        if (date != null) {
            date.setVisibility((on) ? View.VISIBLE : View.GONE);
        }
        toggleDivider();
    }

    public void showWeather(boolean on) {
        weatherLayout.setVisibility((on) ? View.VISIBLE : View.GONE);
        if (weatherLayout2 != null) {
            weatherLayout2.setVisibility((on) ? View.VISIBLE : View.GONE);
        }
        toggleDivider();
    }

    public void setShowNotifications(boolean on) {
        showNotifications = on;
    }

    private void toggleDivider() {
        if (divider == null) return;

        if (!showDivider) {
            divider.setVisibility(GONE);
        }

        boolean shallHide = (
                date != null && date.getVisibility() != VISIBLE
                        && weatherLayout.getVisibility() != VISIBLE
        );
        if (showDivider) {
            divider.setVisibility(shallHide ? INVISIBLE : VISIBLE);
        }
        if (shallHide) {
            setBackgroundColor(Color.parseColor("#00000000"));
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                GradientDrawable shape = new GradientDrawable();
                shape.setCornerRadius(30);
                shape.setColor(Color.parseColor("#44000000"));
                setBackground(shape);
            } else {
                setBackgroundColor(Color.parseColor("#44000000"));
            }
        }
    }

    public void updateLayout(int parentWidth, Configuration config) {
        updateLayout(parentWidth, -1, config, false);
    }

    public void updateLayoutForWidget(int parentWidth, int parentHeight, Configuration config) {
        updateLayout(parentWidth, parentHeight, config, true);
    }

    private void updateLayout(
            int parentWidth, int parentHeight, Configuration config, boolean displayInWidget
    ) {
        if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                        && notificationLayout != null
        ) {
            notificationLayout.setVisibility(
                    mNotificationListener.running
                            && showNotifications
                            && !Settings.useNotificationStatusBar(context)
                            ? VISIBLE : GONE
            );
        }

        final float minFontSize = 8.f; // in sp
        if (layoutId == LAYOUT_ID_DIGITAL) {
            setSize(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (displayInWidget) {
                updateDigitalClockInWidget(parentWidth, parentHeight);
            } else {
                updateDigitalClock(config, parentWidth);
            }
        } else if (layoutId == LAYOUT_ID_DIGITAL2) {
            setSize(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            updateDigitalClock2(config, parentWidth);
        } else if (layoutId == LAYOUT_ID_CALENDAR) {
            setSize(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            setMinimumWidth(7 * Utility.dpToPx(context, 20));
            if (displayInWidget) {
                updateDigitalClockInWidget(parentWidth, parentHeight);
                if (calendarView != null) {
                    // TODO unhide the calendarview
                    calendarView.setVisibility(GONE);
                }
            } else {
                float sizeFactor = 0.8f;
                if (clock != null) {
                    clock.setMaxWidth((int) (sizeFactor * parentWidth));
                    clock.setMaxFontSizesInSp(minFontSize, 60.f);
                }
                if (weatherLayout != null && weatherLayout.getVisibility() == VISIBLE) {
                    weatherLayout.setMaxWidth((int) (sizeFactor * parentWidth));
                    weatherLayout.setMaxFontSizesInSp(6.f, 20.f);
                    weatherLayout.update();
                    weatherLayout.invalidate(); // must invalidate to get correct getHeightOfView below
                }

                if (calendarView != null) {
                    Calendar now = Calendar.getInstance();
                    try {
                        Calendar selected = calendarView.getCurrentDate().getCalendar();
                        if (selected.get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR)) {
                            calendarView.setCurrentDate(now);
                            calendarView.setSelectedDate(now);
                        }
                    } catch (NullPointerException ignored) {
                    }

                    calendarView.setMinimumWidth((int) (0.9 * parentWidth));
                    now.setMinimalDaysInFirstWeek(1);
                    int numWeeksInMonth = now.getActualMaximum(Calendar.WEEK_OF_MONTH);
                    int height = calendarView.getTileHeight() * (numWeeksInMonth + 1 + (calendarView.getTopbarVisible() ? 1 : 0));
                    calendarView.getLayoutParams().height = height;
                }
            }

        } else if (layoutId == LAYOUT_ID_DIGITAL_FLIP) {
            setSize(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (weatherLayout != null && weatherLayout.getVisibility() == VISIBLE) {
                weatherLayout.setMaxWidth((int) (0.9 * parentWidth));
                weatherLayout.setMaxFontSizesInSp(6.f, 20.f);
                weatherLayout.update();
                weatherLayout.invalidate(); // must invalidate to get correct getHeightOfView below
            }
        } else if (layoutId == LAYOUT_ID_ANALOG) {
            setupLayoutAnalog(parentWidth, parentHeight, config, displayInWidget);
        } else {
            setupLayoutAnalog2(parentWidth, parentHeight, config, displayInWidget);
        }

        if (date != null) {
            date.invalidate();
        }
        if (clock != null) {
            clock.invalidate();
        }
    }

    void updateDigitalClock(final Configuration config, int parentWidth) {
        final float minFontSize = 8.f; // in sp
        float widthFactorClock;
        float maxFontSizeClock = 100.f;
        float widthFactor;
        float maxFontSize;

        switch (config.orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                widthFactorClock = 0.3f;

                widthFactor = 0.5f;
                maxFontSize = 20.f;
                break;
            case Configuration.ORIENTATION_PORTRAIT:
            default:
                widthFactorClock = 0.6f;

                widthFactor = 0.8f;
                maxFontSize = 25.f;
                break;
        }
        if (clock != null) {
            clock.setMaxWidth((int) (widthFactorClock * parentWidth));
            clock.setMaxFontSizesInSp(minFontSize, maxFontSizeClock);
        }
        if (date != null) {
            date.setMaxWidth((int) (widthFactor * parentWidth));
            date.setMaxFontSizesInSp(minFontSize, maxFontSize);
        }
        if (weatherLayout != null) {
            weatherLayout.setMaxWidth((int) (widthFactor * parentWidth));
            weatherLayout.setMaxFontSizesInSp(minFontSize, maxFontSize);
            weatherLayout.update();
        }
    }

    void updateDigitalClock2(final Configuration config, int parentWidth) {
        final float minFontSize = 12.f; // in sp
        float widthFactorClock = 0.25f;
        float maxFontSizeClock = 100.f;
        float widthFactor = 0.15f;
        float maxFontSize = 20.f;

        if (clock != null) {
            clock.setMaxWidth((int) (widthFactorClock * parentWidth));
            clock.setMaxFontSizesInSp(minFontSize, maxFontSizeClock);
        }
        if (date != null) {
            date.setMaxWidth((int) (widthFactorClock * parentWidth));
            date.setMaxFontSizesInSp(minFontSize, maxFontSize);
        }
        int iconHeight = -1;
        if (weatherLayout != null) {
            weatherLayout.setMaxWidth((int) (widthFactor * parentWidth));
            //weatherLayout.setMaxFontSizesInSp(minFontSize, maxFontSize);
            weatherLayout.setIconSizeFactor(weatherIconSizeFactor);
            weatherLayout.setTextSize(TypedValue.COMPLEX_UNIT_SP, (int) maxFontSize);
            weatherLayout.update();
            iconHeight = weatherLayout.getIconHeight();
        }
        if (weatherLayout2 != null) {
            weatherLayout2.setMaxWidth((int) (widthFactor * parentWidth));
            weatherLayout2.setTextSize(TypedValue.COMPLEX_UNIT_SP, (int) maxFontSize);
            weatherLayout2.setIconSizeFactor(weatherIconSizeFactor);
            weatherLayout2.setIconHeight(iconHeight);
            //weatherLayout2.setMaxFontSizesInSp(minFontSize, maxFontSize);
            weatherLayout2.update();
        }
    }


    void updateDigitalClockInWidget(int parentWidth, int parentHeight) {
        setPadding(15, 15, 15, 15);
        //ignore orientation, 100% width, so it fills whole space of the widget area
        if (clock != null) {
            clock.setPadding(0, 0, 0, 0);
            clock.setMaxWidth((int) (0.8 * parentWidth));
            clock.setMaxHeight((int) (0.35 * parentHeight));
            clock.setMaxFontSizesInSp(6.f, 300.f);
            clock.invalidate(); // must invalidate to get correct getHeightOfView below
        }
        if (clock_ampm != null) {
            clock_ampm.setPadding(0, 0, 0, 0);
            clock_ampm.setMaxWidth((int) (0.1 * parentWidth));
            clock_ampm.setMaxHeight((int) (0.35 * parentHeight));
            clock_ampm.setMaxFontSizesInSp(6.f, 30.f);
            clock_ampm.invalidate(); // must invalidate to get correct getHeightOfView below
        }
        if (date != null && date.getVisibility() == VISIBLE) {
            date.setMaxWidth((int) (0.9 * parentWidth));
            date.setMaxHeight(parentHeight / 5);
            date.setMaxFontSizesInSp(6.f, 20.f);
            date.invalidate(); // must invalidate to get correct getHeightOfView below
        }
        if (weatherLayout != null && weatherLayout.getVisibility() == VISIBLE) {
            weatherLayout.setMaxWidth((int) (0.9 * parentWidth));
            weatherLayout.setMaxFontSizesInSp(6.f, 20.f);
            weatherLayout.update();
            weatherLayout.invalidate(); // must invalidate to get correct getHeightOfView below
        }

        int measuredHeight = Utility.getHeightOfView(this);
        Log.i(TAG, "### measuredHeight=" + measuredHeight + ", parentHeight=" + parentHeight);

        if (measuredHeight > parentHeight) {
            Log.i(TAG, "### measuredHeight > parentHeight");
            // shrink clock width so that its height fits the widget height
            if (clock != null) {
                clock.setMaxHeight(parentHeight / 4);
            }
            if (date != null) {
                date.setMaxHeight(parentHeight / 6);
            }
            if (weatherLayout != null && weatherLayout.getVisibility() == VISIBLE) {
                weatherLayout.setMaxWidth((int) (0.7 * parentWidth));
                weatherLayout.update();
            }
        }
    }

    private void setSize(int width, int height) {
        getLayoutParams().width = width;
        getLayoutParams().height = height;
        requestLayout();
    }

    public float getAbsScaleFactor() {
        return Math.abs(getScaleX());
    }

    public void setScaleFactor(float factor) {
        setScaleFactor(factor, false);
    }

    public void setScaleFactor(final float factor, final boolean animated) {
        final float sign = mirrorText ? -1.f : 1.f;
        if (animated) {
            animate().setDuration(1000).scaleX(sign * factor).scaleY(factor);
        } else {
            setScaleX(sign * factor);
            setScaleY(factor);
            invalidate();
        }
    }

    private void setupLayoutAnalog(int parentWidth, int parentHeight, Configuration config,
                                   boolean displayInWidget) {
        final float minFontSize = 8.f; // in sp
        final float maxFontSize = 18.f; // in sp
        int widgetSize;
        if (displayInWidget) {
            widgetSize = parentHeight > 0 ? Math.min(parentWidth, parentHeight) : parentWidth;
        } else {
            widgetSize = getAnalogWidgetSize(parentWidth, config);
        }
        if (analog_clock != null) {
            analog_clock.setStyle(AnalogClockConfig.Style.MINIMALISTIC);
            analog_clock.getLayoutParams().height = widgetSize;
            analog_clock.getLayoutParams().width = widgetSize;
        }

        int additionalHeight = 0;
        additionalHeight += notificationLayout.getVisibility() == VISIBLE ? Utility.dpToPx(context, 24.f) : 0;
        additionalHeight += mediaStyleLayout.getVisibility() == VISIBLE ? getHeightOf(mediaStyleLayout) : 0;

        int width = mediaStyleLayout.getVisibility() == VISIBLE ? LayoutParams.WRAP_CONTENT : widgetSize;
        setSize(width, widgetSize + additionalHeight);

        if (date != null) {
            date.setMaxWidth(widgetSize / 2);
            date.setMaxFontSizesInSp(minFontSize, maxFontSize);
            date.setTranslationY(0.2f * widgetSize);
        }
        if (weatherLayout != null) {
            weatherLayout.setMaxWidth(widgetSize / 2);
            weatherLayout.setMaxFontSizesInSp(minFontSize, maxFontSize);
            weatherLayout.update();
            weatherLayout.setTranslationY(-0.2f * widgetSize);
        }
    }

    private void setupLayoutAnalog2(
            int parentWidth, int parentHeight, Configuration config, boolean displayInWidget
    ) {
        switch (layoutId) {
            case LAYOUT_ID_ANALOG:
                analog_clock.setStyle(AnalogClockConfig.Style.MINIMALISTIC);
                break;
            case LAYOUT_ID_ANALOG2:
                analog_clock.setStyle(AnalogClockConfig.Style.SIMPLE);
                break;
            case LAYOUT_ID_ANALOG3:
                analog_clock.setStyle(AnalogClockConfig.Style.ARC);
                break;
            case LAYOUT_ID_ANALOG4:
                analog_clock.setStyle(AnalogClockConfig.Style.DEFAULT);
                break;
        }
        final float minFontSize = (displayInWidget) ? 6f : 10f; // in sp
        final float maxFontSize = 20.f; // in sp

        int widgetSize;
        if (displayInWidget) {
            widgetSize = (parentHeight > 0 && parentHeight < parentWidth) ? parentHeight : parentWidth;
        } else {
            widgetSize = getAnalogWidgetSize(parentWidth, config);
        }

        analog_clock.getLayoutParams().width = widgetSize;
        analog_clock.getLayoutParams().height = widgetSize;

        if (date != null && date.getVisibility() == VISIBLE) {
            date.setMaxWidth(widgetSize / 3 * 2);
            date.setMaxHeight(widgetSize / 10);
            date.setMaxFontSizesInSp(minFontSize, maxFontSize);
            date.invalidate();
        }
        if (weatherLayout != null && weatherLayout.getVisibility() == VISIBLE) {
            weatherLayout.setMaxWidth(widgetSize / 3 * 2);
            weatherLayout.setMaxFontSizesInSp(minFontSize, maxFontSize);
            weatherLayout.update();
            weatherLayout.invalidate();
        }

        int additionalHeight = (int) (getHeightOf(date) + getHeightOf(weatherLayout));
        additionalHeight += notificationLayout.getVisibility() == VISIBLE ? getHeightOf(notificationLayout) : 0;
        additionalHeight += mediaStyleLayout.getVisibility() == VISIBLE ? getHeightOf(mediaStyleLayout) : 0;
        setSize(LinearLayout.LayoutParams.WRAP_CONTENT, widgetSize + additionalHeight);

        int measuredHeight = Utility.getHeightOfView(this);

        if (displayInWidget && parentHeight > 0 && measuredHeight > parentHeight) {
            // shrink analog clock
            int newHeight = parentHeight - additionalHeight;
            LayoutParams params = (LayoutParams) analog_clock.getLayoutParams();
            params.gravity = Gravity.CENTER_HORIZONTAL;
            params.width = newHeight;
            params.height = newHeight;
        }
    }

    private float getHeightOf(View view) {
        if (view == null || view.getVisibility() == GONE) return 0f;
        float height = 1.2f * Utility.getHeightOfView(view);
        Log.i(TAG, String.format("visibility %d", view.getVisibility()));
        Log.i(TAG, String.format("height %f", height));
        return height;
    }

    private int getAnalogWidgetSize(int parentWidth, Configuration config) {
        switch (config.orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                return parentWidth / 4;
            case Configuration.ORIENTATION_PORTRAIT:
            default:
                return parentWidth / 2;
        }
    }

    public void setDateFormat(String formatString) {
        if (date == null) return;
        CustomDigitalClock tdate = (CustomDigitalClock) date;
        tdate.setFormat12Hour(formatString);
        tdate.setFormat24Hour(formatString);
    }

    public void setTimeFormat(String formatString, boolean is24HourFormat) {
        if (clock != null) {
            CustomDigitalClock tclock = (CustomDigitalClock) clock;
            tclock.setCustomFormat(formatString);
        }
        if (clock_ampm != null) {
            CustomDigitalClock tclock = (CustomDigitalClock) clock_ampm;
            tclock.setCustomFormat(is24HourFormat ? "" : "a");
        }
        if (layoutId == LAYOUT_ID_DIGITAL_FLIP) {
            CustomDigitalFlipClock layout = findViewById(R.id.time_layout);
            layout.setCustomIs24Hour(is24HourFormat);
        }
    }

    public void clearWeather() {
        if (weatherLayout == null) return;
        weatherLayout.clear();
    }

    public void update(WeatherEntry entry) {
        if (weatherLayout != null) {
            weatherLayout.update(entry);
        }
        if (weatherLayout2 != null) {
            weatherLayout2.update(entry);
        }
    }
}
