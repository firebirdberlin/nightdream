package com.firebirdberlin.nightdream.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.firebirdberlin.nightdream.CustomAnalogClock;
import com.firebirdberlin.nightdream.CustomDigitalClock;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

public class ClockLayout extends LinearLayout {
    public static final int LAYOUT_ID_DIGITAL = 0;
    public static final int LAYOUT_ID_ANALOG = 1;
    public static final int LAYOUT_ID_ANALOG2 = 2;
    private static final String TAG = "NightDream.ClockLayout";
    private int layoutId = LAYOUT_ID_DIGITAL;

    private Context context = null;
    private AutoAdjustTextView clock = null;
    private AutoAdjustTextView clock_ampm = null;
    private CustomAnalogClock analog_clock = null;
    private AutoAdjustTextView date = null;
    private WeatherLayout weatherLayout = null;
    private View divider = null;

    public ClockLayout(Context context) {
        super(context);
        this.context = context;
    }

    public ClockLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    private void init() {
        if( getChildCount() > 0) {
            removeAllViews();
        }

        LayoutInflater inflater = (LayoutInflater)
            context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View child = null;
        if (layoutId == LAYOUT_ID_DIGITAL) {
            child = inflater.inflate(R.layout.clock_layout, null);
        } else
        if (layoutId == LAYOUT_ID_ANALOG ){
            child = inflater.inflate(R.layout.analog_clock_layout, null);
        } else
        if (layoutId == LAYOUT_ID_ANALOG2 ){
            child = inflater.inflate(R.layout.analog_clock_layout_2, null);
        }
        if (child != null) {
            addView(child);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        return true;
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
        clock = (AutoAdjustTextView) findViewById(R.id.clock);
        clock_ampm = (AutoAdjustTextView) findViewById(R.id.clock_ampm);
        date = (AutoAdjustTextView) findViewById(R.id.date);
        weatherLayout = (WeatherLayout) findViewById(R.id.weatherLayout);
        divider = findViewById(R.id.divider);
        analog_clock = (CustomAnalogClock) findViewById(R.id.analog_clock);
    }

    public void setTypeface(Typeface typeface) {
        if (clock != null) {
            clock.setTypeface(typeface);
        }
        if ( clock_ampm != null ) {
            clock_ampm.setTypeface(typeface);
        }
    }

    public void setPrimaryColor(int color) {
        if (clock != null) {
            clock.setTextColor(color);
        }
        if ( clock_ampm != null ) {
            clock_ampm.setTextColor(color);
        }
        if (analog_clock != null) {
            analog_clock.setPrimaryColor(color);
        }
    }

    public void setSecondaryColor(int color) {
        if (date != null) {
            date.setTextColor(color);
        }
        if (weatherLayout != null ) {
            weatherLayout.setColor(color);
        }
        if (divider != null) {
            divider.setBackgroundColor(color);
        }
        if (analog_clock != null) {
            analog_clock.setSecondaryColor(color);
        }
    }

    public void setTemperature(boolean on, int unit) {
        if (weatherLayout == null) return;
        weatherLayout.setTemperature(on, unit);
    }

    public void setWindSpeed(boolean on, int unit) {
        weatherLayout.setWindSpeed(on, unit);
    }

    public void showDate(boolean on) {
        date.setVisibility( (on) ? View.VISIBLE : View.GONE);
        toggleDivider();
    }

    public void showWeather(boolean on) {
        weatherLayout.setVisibility( (on) ? View.VISIBLE : View.GONE);
        toggleDivider();
    }

    private void toggleDivider() {
        if (divider == null) return;
        if (date.getVisibility() != View.VISIBLE
                && weatherLayout.getVisibility() != View.VISIBLE) {

            divider.setVisibility(View.INVISIBLE);
            setBackgroundColor(Color.parseColor("#00000000"));
        } else {
            divider.setVisibility(View.VISIBLE);
            setBackgroundColor(Color.parseColor("#44000000"));
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void updateLayout(int parentWidth, Configuration config){
        final float minFontSize = 8.f; // in sp

        if (layoutId == LAYOUT_ID_DIGITAL) {
            setSize(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            if (clock != null) {
                clock.setSampleText("22:55");
            }

            switch (config.orientation) {
                case Configuration.ORIENTATION_LANDSCAPE:
                    if (clock != null) {
                        clock.setMaxWidth((int) (0.3f * parentWidth));
                        clock.setMaxFontSizesInSp(minFontSize, (300.f));
                    }
                    if (date != null) {
                        date.setMaxWidth(parentWidth / 2);
                        date.setMaxFontSizesInSp(minFontSize, (20.f));
                    }
                    if (weatherLayout != null) {
                        weatherLayout.setMaxWidth(parentWidth / 2);
                        weatherLayout.setMaxFontSizesInPx(spToPx(minFontSize), spToPx(20.f));
                        weatherLayout.update();
                    }
                    break;
                case Configuration.ORIENTATION_PORTRAIT:
                default:
                    if (clock != null) {
                        clock.setMaxWidth((int) (0.6f * parentWidth));
                        clock.setMaxFontSizesInSp(minFontSize, (300.f));
                    }
                    if (date != null) {
                        date.setMaxWidth((int) (0.8f * parentWidth));
                        date.setMaxFontSizesInSp(minFontSize, (25.f));
                    }
                    if (weatherLayout != null) {
                        weatherLayout.setMaxWidth((int) (0.8f * parentWidth));
                        weatherLayout.setMaxFontSizesInPx(spToPx(minFontSize), spToPx(25.f));
                        weatherLayout.update();
                    }
                    break;
            }
        } else if (layoutId == LAYOUT_ID_ANALOG) {
            int widgetSize = parentWidth/2;

            switch (config.orientation) {
                case Configuration.ORIENTATION_LANDSCAPE:
                    widgetSize = parentWidth/4;
                    break;
                case Configuration.ORIENTATION_PORTRAIT:
                default:
                    widgetSize = parentWidth/2;
                    break;
            }
            setSize(widgetSize, widgetSize);
            if (date != null) {
                date.setMaxWidth(widgetSize / 2);
                date.setMaxFontSizesInSp(minFontSize, (18.f));
                date.setTranslationY(0.2f * widgetSize);
            }
            if (weatherLayout != null) {
                weatherLayout.setMaxWidth(widgetSize / 2);
                weatherLayout.setMaxFontSizesInPx(spToPx(minFontSize), spToPx(18.f));
                weatherLayout.update();
                weatherLayout.setTranslationY(-0.2f * widgetSize);
            }
        }

        if ( date != null ) date.invalidate();
        if (clock != null ) clock.invalidate();
    }

    private void setSize(int width, int height) {
        getLayoutParams().width = width;
        getLayoutParams().height = height;
        requestLayout();
    }

    public void setScaleFactor(float factor) {
        if (Build.VERSION.SDK_INT < 11) return;

        setScaleX(factor);
        setScaleY(factor);
        invalidate();
    }

    private float pixelsToSp(float px) {
        float density = context.getResources().getDisplayMetrics().density;
        return px/density;
    }

    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                                               context.getResources().getDisplayMetrics());
    }

    private int spToPx(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                                               context.getResources().getDisplayMetrics());
    }

    private int pixelsToDp(float px) {
        DisplayMetrics displaymetrics = context.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, px, displaymetrics );
    }

    public void setDateFormat(String formatString) {
        CustomDigitalClock tdate = (CustomDigitalClock) date;
        tdate.setFormat12Hour(formatString);
        tdate.setFormat24Hour(formatString);
    }

    public void setTimeFormat(String formatString12h, String formatString24h) {
        if (clock == null) return;
        CustomDigitalClock tclock = (CustomDigitalClock) clock;
        tclock.setFormat24Hour(formatString24h);
        tclock.setFormat12Hour(formatString12h);
    }

    public void clearWeather() {
        if (weatherLayout == null) return;
        weatherLayout.clear();
    }

    public void update(WeatherEntry entry) {
        if (weatherLayout == null) return;
        weatherLayout.update(entry);
    }
}
