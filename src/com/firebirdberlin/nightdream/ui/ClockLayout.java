package com.firebirdberlin.nightdream.ui;

import java.util.Locale;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.util.DisplayMetrics;
import android.util.Log;

import com.firebirdberlin.nightdream.CustomDigitalClock;
import com.firebirdberlin.nightdream.models.WeatherEntry;
import com.firebirdberlin.nightdream.R;

public class ClockLayout extends LinearLayout {

    private Context context = null;
    private TextView clock = null;
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
        LayoutInflater inflater = (LayoutInflater)
            context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View child = inflater.inflate(R.layout.clock_layout, null);
        addView(child);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        return true;
    }

    @Override
    protected void onFinishInflate() {
        clock = (TextView) findViewById(R.id.clock);
        date = (TextView) findViewById(R.id.date);
        weatherLayout = (WeatherLayout) findViewById(R.id.weatherLayout);
        divider = (View) findViewById(R.id.divider);
    }

    public void setTypeface(Typeface typeface) {
        if (clock == null) return;
        clock.setTypeface(typeface);
    }

    public void setPrimaryColor(int color) {
        if (clock == null) return;
        clock.setTextColor(color);
    }

    public void setSecondaryColor(int color) {
        if (date == null) return;
        date.setTextColor(color);
    }

    public void setTertiaryColor(int color) {
        if (divider == null) return;
        divider.setBackgroundColor(color);
    }

    public void showDate() {
        date.setVisibility(View.VISIBLE);
        divider.setVisibility(View.VISIBLE);
        setBackgroundColor(Color.parseColor("#44000000"));
    }

    public void hideDate() {
        date.setVisibility(View.INVISIBLE);
        divider.setVisibility(View.INVISIBLE);
        setBackgroundColor(Color.parseColor("#00000000"));
    }

    public void setDesiredClockWidth(){
        View parent = (View) getParent();
        int parentWidth = parent.getWidth();
        setDesiredWidth(clock, parentWidth, 0.6f, 300.f);
        setDesiredWidth(date, parentWidth, 0.9f, 30.f);
    }

    public void setDesiredClockWidth(int parentWidth){
        setDesiredWidth(clock, parentWidth, 0.6f, 300.f);
        setDesiredWidth(date, parentWidth, 0.9f, 30.f);
    }

    private void setDesiredWidth(TextView view, int parentWidth, float desiredWidthPercent,
            float maxSp){
        float desiredWidth = desiredWidthPercent * parentWidth;

        String text = view.getText().toString();
        int size = 1;
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        int maxPX = spToPx(maxSp);
        do{
            float textWidth = view.getPaint().measureText(text);
            if (textWidth < desiredWidth) {
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX, ++size);
            } else {
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX, --size);
                break;
            }
        } while(size <= maxPX);
    }

    private float pixelsToSp(float px) {
        float density = context.getResources().getDisplayMetrics().density;
        return px/density;
    }
    private int spToPx(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                                               context.getResources().getDisplayMetrics());
    }
    private int pixelsToDp(float px) {
        DisplayMetrics displaymetrics = context.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, px, displaymetrics );
    }

    public void setTimeFormat() {
        if (clock == null) return;

        // note that format string kk is implemented incorrectly in API <= 17
        // from API level 18 on, we can set the system default
        if (Build.VERSION.SDK_INT >= 18){
            TextClock tclock = (TextClock) clock;
            String tlocalPattern24 = DateFormat.getBestDateTimePattern(Locale.getDefault(), "HH:mm");
            String tlocalPattern12 = DateFormat.getBestDateTimePattern(Locale.getDefault(), "hh:mm a");

            tclock.setFormat12Hour(tlocalPattern12);
            tclock.setFormat24Hour(tlocalPattern24);
        }
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

    public void update(WeatherEntry entry) {
        if (weatherLayout == null) return;
        weatherLayout.update(entry);
    }
}
