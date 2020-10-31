package com.firebirdberlin.nightdream;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.firebirdberlin.nightdream.ui.AutoAdjustTextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CustomDigitalClock extends AutoAdjustTextView {

    Context context;
    TimeReceiver timeReceiver;
    Calendar mCalendar;
    String mFormat;
    private String m12 = "h:mm aa";
    private String m24 = "HH:mm";
    private String mCustom = null;
    private boolean capitalize = false;
    private SimpleDateFormat simpleDateFormat;
    private FormatChangeObserver mFormatChangeObserver;
    private Handler handler;
    private Runnable update = new Runnable() {
        @Override
        public void run() {
            updateTextView();
        }
    };

    public CustomDigitalClock(Context context) {
        super(context);
        this.context = context;
        initClock();
    }

    public CustomDigitalClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomDigitalClock);

        m12 = a.getString(R.styleable.CustomDigitalClock_format12Hr);
        m24 = a.getString(R.styleable.CustomDigitalClock_format24Hr);
        capitalize = a.getBoolean(R.styleable.CustomDigitalClock_capitalize, false);

        a.recycle();

        initClock();
    }

    private void initClock() {
        // Due to an issue with hardware acceleration the text disappears if the font
        // size gets too large. So we disable hardware acceleration.
        // https://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        setFormat();
        updateTextView();
        setClickable(false);
    }

    protected void updateTextView() {
        if (mCalendar == null) {
            mCalendar = Calendar.getInstance();
        }
        mCalendar.setTimeInMillis(System.currentTimeMillis());

        if ("a".equals(mFormat)) {
            try {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) getLayoutParams();

                if (params != null) {
                    int hour = mCalendar.get(Calendar.HOUR_OF_DAY);
                    params.gravity = (hour < 12) ? Gravity.BOTTOM : Gravity.TOP;
                }
            }catch (Exception e){
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) getLayoutParams();
                if (params != null) {
                    int hour = mCalendar.get(Calendar.HOUR_OF_DAY);
                    if (hour < 12) {
                        params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;
                        ViewGroup params2 = (ViewGroup) getParent();
                        params.topToTop = params2.findViewById(R.id.clock).getId();
                    } else{
                        params.topToTop = ConstraintLayout.LayoutParams.UNSET;
                        ViewGroup params2 = (ViewGroup) getParent();
                        params.bottomToBottom = params2.findViewById(R.id.clock).getId();
                    }
                }
            }
        }

        String text = simpleDateFormat.format(mCalendar.getTime());
        if (text != getText() ) {
            setText(text);
            //invalidate();
        }

        if (mFormat.contains(":ss")) {
            if (handler == null) {
                handler = new Handler();
            }
            handler.removeCallbacks(update);
            long now = System.currentTimeMillis();
            long delta = 1000 - now / 1000 * 1000;
            handler.postDelayed(update, delta);
        }
    }

    @Override
    public void onAttachedToWindow(){
        super.onAttachedToWindow();
        setTimeTick();

        mFormatChangeObserver = new FormatChangeObserver();
        getContext().getContentResolver().registerContentObserver(
                Settings.System.CONTENT_URI, true, mFormatChangeObserver);

    }

    @Override
    public void onDetachedFromWindow(){
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
        return android.text.format.DateFormat.is24HourFormat(getContext());
    }

    private void setFormat() {
        if (mCustom != null ) {
           mFormat = mCustom;
        } else
        if (get24HourMode()) {
            mFormat = m24;
        } else {
            mFormat = m12;
        }
        simpleDateFormat = new SimpleDateFormat(mFormat);
        setSampleTime();
    }

    void setSampleTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 22);
        cal.set(Calendar.MINUTE, 55);
        cal.set(Calendar.SECOND, 55);
        String text = simpleDateFormat.format(cal.getTime());
        setSampleText(text);
    }

    public void setCustomFormat(String format) {
        this.mCustom = format;
        setFormat();
        updateTextView();
    }

    public void setFormat12Hour(String format) {
        this.m12 = format;
        setFormat();
        updateTextView();
    }

    public void setFormat24Hour(String format) {
        this.m24 = format;
        setFormat();
        updateTextView();
    }

    class TimeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {
            updateTextView();
        }
    }

    private class FormatChangeObserver extends ContentObserver {
        public FormatChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            setFormat();
        }
    }

    @Override
    public void invalidate() {
        if (capitalize) {
            String text = capitalize(getText().toString());
            setText(text);
        }
        super.invalidate();
    }

    private String capitalize(String input) {
        if (input == null) {
            return null;
        }
        if (input.length() > 1 ) {
            return input.substring(0, 1).toUpperCase() + input.substring(1);
        } else
        if (input.length() > 0 ) {
            return input.substring(0, 1).toUpperCase();
        }

        return input;
    }
}
