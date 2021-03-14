package com.firebirdberlin.nightdream.ui;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class CustomDigitalAnimClock extends LinearLayout {

    private static final String TAG = "CustomDigitalAnimClock";
    private final Runnable update = () -> updateTextView();
    private Handler handler;

    TimeReceiver timeReceiver;
    private Context context;
    private FormatChangeObserver mFormatChangeObserver;
    private Boolean customIs24Hour = null;
    private AnimDigit mCharHighHour;
    private AnimDigit mCharLowHour;
    private AnimDigit mCharHighMinute;
    private AnimDigit mCharLowMinute;
    private AnimDigit mCharHighSecond;
    private AnimDigit mCharLowSecond;
    private TextView colon;
    private LinearLayout secondsLayout;
    private int currentHourHigh = -1;
    private int currentHourLow = -1;
    private int currentMinuteHigh = -1;
    private int currentMinuteLow = -1;
    private int currentSecondHigh = -1;
    private int currentSecondLow = -1;
    String mFormat;
    private String m12 = "h:mm aa";
    private String m24 = "HH:mm";
    private String mCustom = null;

    public CustomDigitalAnimClock(Context context) {
        super(context);
        this.context = context;
        initLayout();
        init();
    }

    public CustomDigitalAnimClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        initLayout();
        init();
    }

    private void initLayout() {
        Log.d(TAG,"initLayout()");

        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child = inflater.inflate(R.layout.custom_digital_anim_clock, null);

        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(child, lp);
        mCharHighHour = findViewById(R.id.charHighHour);
        mCharLowHour = findViewById(R.id.charLowHour);
        mCharHighMinute = findViewById(R.id.charHighMinute);
        mCharLowMinute = findViewById(R.id.charLowMinute);
        mCharHighSecond = findViewById(R.id.charHighSecond);
        mCharLowSecond = findViewById(R.id.charLowSecond);
        secondsLayout = findViewById(R.id.secondsLayout);
        colon = findViewById(R.id.colon);
    }

    private void init() {
        setFormat();
        resume();
    }

    private void resume() {
        Calendar time = Calendar.getInstance();
        int hour = time.get(get24HourMode() ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        int highHour = hour / 10;
        if (!get24HourMode()) {
            highHour = (hour == 0) ? 1 : highHour;
        }
        int lowHour = (hour - highHour * 10);

        int minutes = time.get(Calendar.MINUTE);
        int highMinute = minutes / 10;
        int lowMinute = (minutes - highMinute * 10);

        int seconds = time.get(Calendar.SECOND);
        int highSecond = seconds / 10;
        int lowSecond = (seconds - highSecond * 10);

        mCharHighHour.setChar(highHour);
        mCharLowHour.setChar(lowHour);
        mCharHighMinute.setChar(highMinute);
        mCharLowMinute.setChar(lowMinute);
        mCharHighSecond.setChar(highSecond);
        mCharLowSecond.setChar(lowSecond);
        currentHourHigh = highHour;
        currentHourLow = lowHour;
        currentMinuteHigh = highMinute;
        currentMinuteLow = lowMinute;
        currentSecondHigh = highSecond;
        currentSecondLow = lowSecond;
        updateTextView();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setTimeTick();
        mFormatChangeObserver = new FormatChangeObserver();
        getContext().getContentResolver().registerContentObserver(
                Settings.System.CONTENT_URI, true, mFormatChangeObserver);
        resume();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (timeReceiver != null) {
            try {
                context.unregisterReceiver(timeReceiver);
            } catch (IllegalArgumentException e) {
                // receiver was not registered,
            }
            timeReceiver = null;
        }
        if (mFormatChangeObserver != null) {
            ContentResolver cr = getContext().getContentResolver();
            cr.unregisterContentObserver(mFormatChangeObserver);
        }
        if (handler != null) {
            handler.removeCallbacks(update);
        }
    }

    void setTimeTick() {
        timeReceiver = new TimeReceiver();
        context.registerReceiver(timeReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    /**
     * Pulls 12/24 mode from system settings
     */
    private boolean get24HourMode() {
        if (customIs24Hour != null) {
            return customIs24Hour;
        }
        return android.text.format.DateFormat.is24HourFormat(getContext());
    }

    public void setCustomIs24Hour(boolean is24Hour) {
        Log.i(TAG, "Settings custom 24 hour format: " + is24Hour);
        this.customIs24Hour = is24Hour;
        init();
        invalidate();
    }

    public void setCustomFormat(String format) {
        this.mCustom = format;
        setFormat();
        updateTextView();
    }

    private void setFormat() {
        if (mCustom != null) {
            mFormat = mCustom;
        } else if (get24HourMode()) {
            mFormat = m24;
        } else {
            mFormat = m12;
        }
    }

    public void setPrimaryColor(int color) {
        mCharHighHour.setTextColor(color);
        mCharLowHour.setTextColor(color);
        mCharHighMinute.setTextColor(color);
        mCharLowMinute.setTextColor(color);
        mCharHighSecond.setTextColor(color);
        mCharLowSecond.setTextColor(color);
        colon.setTextColor(color);
        invalidate();
    }

    public void setSecondaryColor(int color) {
        invalidate();
    }

    protected void updateTextView() {
        Calendar time = Calendar.getInstance();
        int hour = time.get(get24HourMode() ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        int highHour = hour / 10;
        if (!get24HourMode()) {
            highHour = (hour == 0) ? 1 : highHour;
        }
        int lowHour = (hour - highHour * 10);

        int minutes = time.get(Calendar.MINUTE);
        int highMinute = minutes / 10;
        int lowMinute = (minutes - highMinute * 10);

        int seconds = time.get(Calendar.SECOND);
        int highSecond = seconds / 10;
        int lowSecond = (seconds - highSecond * 10);

        if (currentHourHigh != highHour) {
            mCharHighHour.start(highHour);
        }
        if (currentHourLow != lowHour) {
            mCharLowHour.start(lowHour);
        }
        if (currentMinuteHigh != highMinute) {
            mCharHighMinute.start(highMinute);
        }
        if (currentMinuteLow != lowMinute) {
            mCharLowMinute.start(lowMinute);
        }
        if (currentSecondHigh != highSecond) {
            mCharHighSecond.start(highSecond);
        }
        if (currentSecondLow != lowSecond) {
            mCharLowSecond.start(lowSecond);
        }

        currentHourHigh = highHour;
        currentHourLow = lowHour;
        currentMinuteHigh = highMinute;
        currentMinuteLow = lowMinute;
        currentSecondHigh = highSecond;
        currentSecondLow = lowSecond;

        if (mFormat != null && !(mFormat.contains("hh") || mFormat.contains("HH")) && (currentHourHigh == 0)) {
            mCharHighHour.setVisibility(View.GONE);
        }
        else {
            mCharHighHour.setVisibility(View.VISIBLE);
        }

        if (mFormat != null && mFormat.contains(":ss")) {
            secondsLayout.setVisibility(View.VISIBLE);
            if (handler == null) {
                handler = new Handler();
            }
            handler.removeCallbacks(update);
            long now = System.currentTimeMillis();
            long delta = 1000 - now / 1000 * 1000;
            handler.postDelayed(update, delta);
        } else {
            if (handler != null) {
                handler.removeCallbacks(update);
            }
            secondsLayout.setVisibility(View.GONE);
        }
    }

    class TimeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {
            Log.d(TAG, "TimeReceiver");
            updateTextView();
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
    }

    private class FormatChangeObserver extends ContentObserver {
        public FormatChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            init();
        }
    }
}
