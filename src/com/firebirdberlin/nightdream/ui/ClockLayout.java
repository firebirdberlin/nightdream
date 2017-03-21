package com.firebirdberlin.nightdream.ui;

import java.lang.Runnable;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.util.DisplayMetrics;

import com.firebirdberlin.nightdream.CustomDigitalClock;
import com.firebirdberlin.nightdream.CustomAnalogClock;
import com.firebirdberlin.nightdream.models.WeatherEntry;
import com.firebirdberlin.nightdream.R;

public class ClockLayout extends LinearLayout {
    private static final String TAG = "NightDream.ClockLayout";

    public static final int LAYOUT_ID_DIGITAL = 0;
    public static final int LAYOUT_ID_ANALOG = 1;
    private int layoutId = LAYOUT_ID_DIGITAL;

    private Context context = null;
    private TextView clock = null;
    private TextView clock_ampm = null;
    private CustomAnalogClock analog_clock = null;
    private TextView date = null;
    private WeatherLayout weatherLayout = null;
    private View divider = null;

    public ClockLayout(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public ClockLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init() {
        if( getChildCount() > 0) {
            removeAllViews();
        }

        LayoutInflater inflater = (LayoutInflater)
            context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View child;
        if (layoutId == LAYOUT_ID_DIGITAL) {
            child = inflater.inflate(R.layout.clock_layout, null);
        } else {
            child = inflater.inflate(R.layout.analog_clock_layout, null);
        }
        addView(child);
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
        Log.v(TAG, "onFinishInflate");
        clock = (TextView) findViewById(R.id.clock);
        clock_ampm = (TextView) findViewById(R.id.clock_ampm);
        date = (TextView) findViewById(R.id.date);
        weatherLayout = (WeatherLayout) findViewById(R.id.weatherLayout);
        divider = (View) findViewById(R.id.divider);
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

    public void updateLayout(int parentWidth, Configuration config){
        if (layoutId == LAYOUT_ID_DIGITAL) {

            setSize(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            switch (config.orientation) {
                case Configuration.ORIENTATION_LANDSCAPE:
                    setDesiredWidth(clock, parentWidth, 0.3f, 300.f);
                    setDesiredWidth(date, parentWidth, 0.5f, 20.f);
                    break;
                case Configuration.ORIENTATION_PORTRAIT:
                default:
                    setDesiredWidth(clock, parentWidth, 0.6f, 300.f);
                    setDesiredWidth(date, parentWidth, 0.9f, 25.f);
                    break;
            }
            if (date.getVisibility() == View.VISIBLE) {
                float textSize = date.getTextSize();
                weatherLayout.setTextSize(TypedValue.COMPLEX_UNIT_PX, (int) textSize);
            }
        } else {
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
            setDesiredWidth(date, widgetSize, 0.5f, 18.f);
            weatherLayout.setMaxWidth(widgetSize/2);
            weatherLayout.setMaxFontSizeInPx(spToPx(18.f));
            weatherLayout.update();

            weatherLayout.setTranslationY(-0.2f * widgetSize);
            date.setTranslationY(0.2f * widgetSize);
        }

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

    private void setDesiredWidth(TextView view, int parentWidth, float desiredWidthPercent,
            float maxSp){
        if (view == null) return;
        float desiredWidth = desiredWidthPercent * parentWidth;

        String text = view.getText().toString();
        int size = 1;
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        int maxPX = spToPx(maxSp);
        do{
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, ++size);
        } while(size <= maxPX && view.getPaint().measureText(text) < desiredWidth);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, --size);
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
        if (Build.VERSION.SDK_INT >= 17){
            TextClock tdate  = (TextClock) date;
            tdate.setFormat12Hour(formatString);
            tdate.setFormat24Hour(formatString);

        } else {
            CustomDigitalClock tdate = (CustomDigitalClock) date;
            tdate.setFormat12Hour(formatString);
            tdate.setFormat24Hour(formatString);
        }
    }

    public void clearWeather() {
        weatherLayout.clear();
    }

    public void update(WeatherEntry entry) {
        if (weatherLayout == null) return;
        weatherLayout.update(entry);
    }
}
