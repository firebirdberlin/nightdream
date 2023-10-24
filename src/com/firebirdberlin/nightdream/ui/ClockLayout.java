package com.firebirdberlin.nightdream.ui;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
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

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator;

import com.firebirdberlin.nightdream.CustomAnalogClock;
import com.firebirdberlin.nightdream.CustomDigitalClock;
import com.firebirdberlin.nightdream.Graphics;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.databinding.NotificationMediacontrolBinding;
import com.firebirdberlin.nightdream.mNotificationListener;
import com.firebirdberlin.nightdream.models.AnalogClockConfig;
import com.firebirdberlin.nightdream.models.FontCache;
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
    public static final int LAYOUT_ID_DIGITAL3 = 8;
    public static final int LAYOUT_ID_DIGITAL_ANIMATED = 9;
    private static final String TAG = "NightDream.ClockLayout";
    private final WeatherLayout[] weatherLayouts = {null, null, null};
    private final Context context;
    private int layoutId = LAYOUT_ID_DIGITAL;
    private AutoAdjustTextView clock = null;
    private MaterialCalendarView calendarView = null;
    private AutoAdjustTextView clock_ampm = null;
    private CustomAnalogClock analog_clock = null;
    private AutoAdjustTextView date = null;
    private WeatherLayout weatherLayout = null;
    private FlexboxLayout notificationLayout = null;
    private ConstraintLayout mediaStyleLayout = null;
    private ConstraintLayout pollenLayout = null;
    private View divider = null;
    private boolean showDivider = true;
    private boolean showWeather = false;
    private boolean mirrorText = false;
    private boolean showNotifications = true;
    private int weatherIconSizeFactor = 3;
    private int oldPrimaryColor = 0;
    private int dateInvisibilityMethod = GONE;
    private int backgroundTransparency = 100;

    public ClockLayout(Context context) {
        super(context);
        this.context = context;
    }

    public ClockLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public void setBackgroundTransparency(int backgroundTransparency) {
        this.backgroundTransparency = backgroundTransparency;
    }

    private void init() {

        if (getChildCount() > 0) {
            removeAllViews();
        }
        dateInvisibilityMethod = (layoutId == LAYOUT_ID_DIGITAL2) ? INVISIBLE : GONE;
        ContextThemeWrapper ctxThemeWrapper = new ContextThemeWrapper(context, R.style.ActivityTheme);
        LayoutInflater inflater  = LayoutInflater.from(ctxThemeWrapper);
        View child;
        if (layoutId == LAYOUT_ID_DIGITAL) {
            child = inflater.inflate(R.layout.clock_layout, null);
        } else if (layoutId == LAYOUT_ID_DIGITAL2) {
            child = inflater.inflate(R.layout.clock_layout_digital2, null);
        } else if (layoutId == LAYOUT_ID_DIGITAL3) {
            child = inflater.inflate(R.layout.clock_layout_digital3, null);
        } else if (layoutId == LAYOUT_ID_DIGITAL_FLIP) {
            child = inflater.inflate(R.layout.clock_layout_digital_flip, null);
        } else if (layoutId == LAYOUT_ID_DIGITAL_ANIMATED) {
            child = inflater.inflate(R.layout.clock_layout_anim, null);
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
        weatherLayouts[0] = weatherLayout;
        weatherLayouts[1] = findViewById(R.id.weatherLayout2);
        weatherLayouts[2] = findViewById(R.id.weatherLayout3);

        divider = findViewById(R.id.divider);
        analog_clock = findViewById(R.id.analog_clock);
        notificationLayout = findViewById(R.id.notificationbar);
        mediaStyleLayout = findViewById(R.id.notification_mediacontrol_bar);
        pollenLayout = findViewById(R.id.pollen_container);

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

        if (
                typeface != null
                        && !typeface.equals(FontCache.get(context, "fonts/dseg14classic.ttf"))
                        && !typeface.equals(FontCache.get(context, "fonts/7_segment_digital.ttf"))
        ) {
            if (date != null) {
                date.setTypeface(typeface);
            }
            for (WeatherLayout layout : weatherLayouts) {
                if (layout != null) {
                    layout.setTypeface(typeface);
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    public void setPrimaryColor(
            int color, final int glowRadius, final int glowColor, final int textureId,
            boolean animated
    ) {
        if (animated) {
            ValueAnimator anim = new ValueAnimator();
            anim.setIntValues(oldPrimaryColor, color);
            anim.setEvaluator(new ArgbEvaluator());
            oldPrimaryColor = color;
            anim.addUpdateListener(valueAnimator -> {
                Integer animatedColor = (Integer) valueAnimator.getAnimatedValue();
                if (animatedColor == null) return;
                setPrimaryColor(animatedColor);
            });
            anim.setDuration(1000);
            anim.start();
        } else {
            setPrimaryColor(color);
        }

        applyTexture(clock, glowRadius, glowColor, textureId);
        applyTexture(clock_ampm, glowRadius, glowColor, textureId);
    }

    private void setPrimaryColor(int color) {
        if (clock != null) {
            clock.setTextColor(color);
        }
        if (clock_ampm != null) {
            clock_ampm.setTextColor(color);
        }
        if (analog_clock != null) {
            analog_clock.setPrimaryColor(color);
        }

        if (layoutId == LAYOUT_ID_DIGITAL_FLIP) {
            CustomDigitalFlipClock layout = findViewById(R.id.time_layout);
            layout.setPrimaryColor(color);
        }
        if (layoutId == LAYOUT_ID_DIGITAL_ANIMATED) {
            CustomDigitalAnimClock layout = findViewById(R.id.time_layout);
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
        for (WeatherLayout layout : weatherLayouts) {
            if (layout != null) {
                layout.setColor(color);
            }
        }
        if (divider != null) {
            divider.setBackgroundColor(color);
        } else {
            View v = findViewWithTag("divider");
            if (v != null) {
                v.setBackgroundColor(color);
            }
        }

        if (analog_clock != null) {
            analog_clock.setSecondaryColor(color);
        }

        if (layoutId == LAYOUT_ID_DIGITAL_FLIP) {
            CustomDigitalFlipClock layout = findViewById(R.id.time_layout);
            layout.setSecondaryColor(color);
        }

        if (mediaStyleLayout != null) {
            View boundView = mediaStyleLayout.getChildAt(0);
            NotificationMediacontrolBinding mediaControlLayoutBinding = DataBindingUtil.getBinding(boundView);

            if (mediaControlLayoutBinding != null) {
                mediaControlLayoutBinding.getModel().setColor(color);
                mediaControlLayoutBinding.invalidateAll();
            }
        }

        Utility.colorizeView(calendarView, color, PorterDuff.Mode.MULTIPLY);
        Utility.colorizeView(notificationLayout, color);
    }

    public void setTemperature(boolean on, boolean withApparentTemperature, int unit) {
        for (WeatherLayout layout : weatherLayouts) {
            if (layout != null) {
                layout.setTemperature(on, withApparentTemperature, unit);
            }
        }
    }

    public void setWindSpeed(boolean on, int unit) {
        for (WeatherLayout layout : weatherLayouts) {
            if (layout != null) {
                layout.setWindSpeed(on, unit);
            }
        }
    }

    public void setWeatherLocation(boolean on) {
        for (WeatherLayout layout : weatherLayouts) {
            if (layout != null) {
                layout.setLocation(on);
            }
        }
    }

    public void setWeatherIconMode(int weatherIconMode) {
        for (WeatherLayout layout : weatherLayouts) {
            if (layout != null) {
                layout.setWeatherIconMode(weatherIconMode);
            }
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
            date.setVisibility((on) ? View.VISIBLE : dateInvisibilityMethod);
        }
        toggleDivider();
    }

    public void showWeather(boolean on) {
        this.showWeather = on;
        for (WeatherLayout layout : weatherLayouts) {
            if (layout != null) {
                layout.setVisibility((on) ? View.VISIBLE : View.GONE);
            }
        }
        toggleDivider();
    }


    public void showPollenExposure(boolean on) {
        if (pollenLayout != null) {
            pollenLayout.setVisibility((on) ? View.VISIBLE : GONE);
        }
    }

    public void setShowNotifications(boolean on) {
        showNotifications = on;
    }

    private void toggleDivider() {
        setupBackground(null);
    }

    public void setupBackground(Drawable backgroundDrawable) {
        boolean shallHide = (
                date != null && date.getVisibility() != VISIBLE
                        && weatherLayout.getVisibility() != VISIBLE
        );
        if (divider != null) {
            divider.setVisibility(!showDivider || shallHide ? INVISIBLE : VISIBLE);
        }

        if (backgroundDrawable != null) {
            setBackground(backgroundDrawable);
            return;
        }

        if (shallHide) {
            setBackgroundColor(Color.parseColor("#00000000"));
        } else {
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(30);
            int color = Color.parseColor("#44000000");
            color = Graphics.setColorWithAlpha(color, backgroundTransparency);
            shape.setColor(color);
            setBackground(shape);
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
            updateDigitalClock2(parentWidth);
        } else if (layoutId == LAYOUT_ID_DIGITAL3) {
            updateDigitalClock3(displayInWidget, parentWidth);
        } else if (layoutId == LAYOUT_ID_CALENDAR) {
            updateDigitalClockCalendar(displayInWidget, parentWidth, parentHeight, minFontSize);
        } else if (layoutId == LAYOUT_ID_DIGITAL_FLIP) {
            updateDigitalFlipClock(parentWidth);
        } else if (layoutId == LAYOUT_ID_DIGITAL_ANIMATED) {
            updateDigitalFlipClock(parentWidth);
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

        for (WeatherLayout layout : weatherLayouts) {
            if (layout != null) {
                layout.setMaxWidth((int) (widthFactor * parentWidth));
                layout.setMaxFontSizesInSp(minFontSize, maxFontSize);
                layout.update();
            }
        }
    }

    void updateDigitalClock2(int parentWidth) {
        final float minFontSize = 12.f; // in sp
        float widthFactorClock = 0.25f;
        float maxFontSizeClock = 100.f;
        float widthFactor = 0.15f;
        float maxFontSize = 20.f;

        if (clock != null) {
            clock.setMaxWidth((int) (widthFactorClock * parentWidth));
            clock.setMaxFontSizesInSp(minFontSize, maxFontSizeClock);
        }
        int dateTextSize = -1;
        if (date != null) {
            date.setMaxWidth((int) (widthFactorClock * parentWidth));
            date.setMaxFontSizesInSp(minFontSize, maxFontSize);
            dateTextSize = (int) date.getTextSize();
        }

        int iconHeight = -1;
        for (WeatherLayout layout : weatherLayouts) {
            if (layout == null) continue;
            layout.setMaxWidth((int) (widthFactor * parentWidth));
            layout.setIconSizeFactor(weatherIconSizeFactor);
            if (dateTextSize > 0) {
                layout.setTypeface(date.getTypeface());
                layout.setTextSizePx(dateTextSize);
            } else {
                int sizePx = Utility.spToPx(context, maxFontSize);
                layout.setTextSizePx(sizePx);
            }
            if (iconHeight < 0) {
                iconHeight = layout.getIconHeight();
            } else {
                layout.setIconHeight(iconHeight);
            }
            layout.update();
        }
    }

    void updateDigitalClock3(final boolean displayInWidget, int parentWidth) {
        float maxWidth = displayInWidget ? 0.4f * parentWidth : 0.25f * parentWidth;

        float sizeFactor = displayInWidget ? 1.f : 0.6f;
        setSize(
                showWeather ? (int) (sizeFactor * parentWidth) : LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        {
            View v = findViewById(R.id.time_layout);
            v.getLayoutParams().width = (int) maxWidth;
            v.requestLayout();
        }

        {
            View v = findViewById(R.id.weather_container);
            v.getLayoutParams().width = showWeather ? (int) maxWidth : 0;
            v.requestLayout();
            View divider = findViewWithTag("divider");
            divider.setVisibility(showWeather ? View.VISIBLE : View.GONE);
        }

        if (clock != null) {
            clock.setMaxWidth((int) maxWidth);
            clock.setMaxFontSizesInSp(10.f, 50.f);
            clock.invalidate();
        }
        if (date != null) {
            date.setMaxWidth((int) maxWidth);
            date.setMaxFontSizesInSp(10.f, 20.f);
            date.invalidate();
        }


        if (weatherLayout != null && clock != null) {
            float textSize = (float) Utility.pixelsToDp(context, clock.getTextSize());
            weatherLayout.setMaxWidth((int) (maxWidth));
            weatherLayout.setMaxHeight((Utility.getHeightOfView(clock)));
            weatherLayout.setMaxFontSizesInSp(10.f, textSize);
            weatherLayout.update();
        }
        for (int i = 1; i < weatherLayouts.length; i++) {
            WeatherLayout layout = weatherLayouts[i];
            if (layout != null) {
                float textSize = (float) Utility.pixelsToDp(context, date.getTextSize());
                layout.setLocation(true);
                layout.setMaxWidth((int) maxWidth);
                layout.setMaxFontSizesInSp(10.f, textSize);
                layout.update();
            }
        }

        if (displayInWidget) {
            setDividerHeight(Utility.getHeightOfView(this));
        } else {
            final View container = findViewById(R.id.grid_layout);
            container.post(() -> setDividerHeight((int) (.9f * container.getHeight())));
        }
        requestLayout();
    }

    void setDividerHeight(int height) {
        if (!showWeather) {
            return;
        }
        View divider = findViewWithTag("divider");
        divider.getLayoutParams().height = height;
        divider.invalidate();
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
        float fontSize = -1;
        for (WeatherLayout layout : weatherLayouts) {
            if (layout != null && layout.getVisibility() == VISIBLE) {
                if (fontSize == -1) {
                    layout.setMaxWidth((int) (0.9 * parentWidth));
                    layout.setMaxFontSizesInSp(6.f, 20.f);
                } else {
                    layout.setTextSizePx((int) fontSize);
                }
                layout.update();
                layout.invalidate();
                fontSize = layout.getTextSize();
            }
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

            for (WeatherLayout layout : weatherLayouts) {
                if (layout != null && layout.getVisibility() == VISIBLE) {
                    layout.setMaxWidth((int) (0.7 * parentWidth));
                    layout.update();
                }
            }
        }
    }

    void updateDigitalFlipClock(int parentWidth) {
        setSize(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        float fontSize = -1;
        for (WeatherLayout layout : weatherLayouts) {
            if (layout != null && layout.getVisibility() == VISIBLE) {
                if (fontSize == -1) {
                    layout.setMaxWidth((int) (0.9 * parentWidth));
                    layout.setMaxFontSizesInSp(6.f, 16.f);
                } else {
                    layout.setTextSizePx((int) fontSize);
                }
                layout.update();
                layout.invalidate();
                fontSize = layout.getTextSize();
            }
        }
    }

    void updateDigitalClockCalendar(boolean displayInWidget, int parentWidth, int parentHeight, float minFontSize) {
        setSize(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int minWidth = Utility.dpToPx(context, 200);
        setMinimumWidth(minWidth);
        if (displayInWidget) {
            if (calendarView != null) {
                // TODO unhide the calendarview
                calendarView.setVisibility(GONE);
            }
            updateDigitalClockInWidget(parentWidth, parentHeight);
        } else {

            float sizeFactor = 0.8f;
            if (clock != null) {
                clock.setMaxWidth((int) (0.7f * minWidth));
                clock.setMaxFontSizesInSp(minFontSize, 50);
            }

            float fontSize = -1;
            for (WeatherLayout layout : weatherLayouts) {
                if (layout != null && layout.getVisibility() == VISIBLE) {
                    if (fontSize == -1) {
                        layout.setMaxWidth((int) (sizeFactor * parentWidth));
                        layout.setMaxFontSizesInSp(6.f, 20.f);
                    } else {
                        layout.setTextSizePx((int) fontSize);
                    }
                    layout.update();
                    layout.invalidate(); // must invalidate to get correct getHeightOfView below
                    fontSize = layout.getTextSize();
                }
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

    private void setupLayoutAnalog(
            int parentWidth, int parentHeight, Configuration config, boolean displayInWidget
    ) {
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
        additionalHeight += pollenLayout.getVisibility() == VISIBLE ? pollenLayout.getHeight() : 0;

        int width = mediaStyleLayout.getVisibility() == VISIBLE ? LayoutParams.WRAP_CONTENT : widgetSize;
        setSize(width, widgetSize + additionalHeight);

        if (date != null) {
            date.setMaxWidth(widgetSize / 2);
            date.setMaxFontSizesInSp(minFontSize, maxFontSize);
            date.setTranslationY(0.2f * widgetSize);
        }
        for (WeatherLayout layout : weatherLayouts) {
            if (layout != null) {
                layout.setMaxWidth(widgetSize / 2);
                layout.setMaxFontSizesInSp(minFontSize, maxFontSize);
                layout.update();
                layout.setTranslationY(-0.2f * widgetSize);
            }
        }
    }

    private void setupLayoutAnalog2(
            int parentWidth, int parentHeight, Configuration config, boolean displayInWidget
    ) {
        switch (layoutId) {
            case LAYOUT_ID_ANALOG:
                analog_clock.setStyle(AnalogClockConfig.Style.MINIMALISTIC, !displayInWidget);
                break;
            case LAYOUT_ID_ANALOG2:
                analog_clock.setStyle(AnalogClockConfig.Style.SIMPLE, !displayInWidget);
                break;
            case LAYOUT_ID_ANALOG3:
                analog_clock.setStyle(AnalogClockConfig.Style.ARC, !displayInWidget);
                break;
            case LAYOUT_ID_ANALOG4:
                analog_clock.setStyle(AnalogClockConfig.Style.DEFAULT, !displayInWidget);
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

        int additionalHeight = (int) getHeightOf(date);
        float fontSize = -1;
        for (WeatherLayout layout : weatherLayouts) {
            if (layout != null && layout.getVisibility() == VISIBLE) {
                if (fontSize == -1) {
                    layout.setMaxWidth(widgetSize / 3 * 2);
                    layout.setMaxFontSizesInSp(minFontSize, maxFontSize);
                } else {
                    layout.setTextSizePx((int) fontSize);
                }
                layout.update();
                layout.invalidate();
                fontSize = layout.getTextSize();

                additionalHeight += getHeightOf(layout);
            }
        }

        additionalHeight += notificationLayout.getVisibility() == VISIBLE ? getHeightOf(notificationLayout) : 0;
        additionalHeight += mediaStyleLayout.getVisibility() == VISIBLE ? getHeightOf(mediaStyleLayout) : 0;
        additionalHeight += pollenLayout.getVisibility() == VISIBLE ? pollenLayout.getHeight() : 0;
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
            layout.setCustomFormat(formatString);
        }
        if (layoutId == LAYOUT_ID_DIGITAL_ANIMATED) {
            CustomDigitalAnimClock layout = findViewById(R.id.time_layout);
            layout.setCustomIs24Hour(is24HourFormat);
            layout.setCustomFormat(formatString);
        }

    }

    public void clearWeather() {
        for (WeatherLayout layout : weatherLayouts) {
            if (layout != null) {
                layout.clear();
            }
        }
    }

    public void update(WeatherEntry entry, boolean displayInWidget) {
        Log.i(TAG, "update(WeatherEntry) " + entry.cityName);
        for (WeatherLayout layout : weatherLayouts) {
            if (layout != null) {
                layout.setWidget(displayInWidget);
                layout.update(entry);
            }
        }
    }

    public void getScaledSize(int[] size) {
        size[0] = Math.abs((int) (getWidth() * getScaleX()));
        size[1] = Math.abs((int) (getHeight() * getScaleY()));
    }

    public float getScaledWidth() {
        return getWidth() * getScaleX();
    }

    public float getScaledHeight() {
        return getHeight() * getScaleY();
    }
}
