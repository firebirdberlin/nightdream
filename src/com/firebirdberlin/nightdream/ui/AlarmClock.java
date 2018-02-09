package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.SetAlarmClockActivity;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.services.AlarmHandlerService;


public class AlarmClock extends RelativeLayout {
    private static String TAG ="NightDream.AlarmClock";

    private boolean daydreamMode = false;
    private int customSecondaryColor = Color.parseColor("#C2C2C2");

    private TextView alarmTimeTextView;
    private AlarmClockView alarmClockView;

    public AlarmClock(Context context, AttributeSet attrs) {
        super(context, attrs);

        initColorFilters();

        alarmClockView = new AlarmClockView(context, attrs);
        LayoutParams layoutAlarmClockView = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        alarmTimeTextView = new TextView(context);
        alarmTimeTextView.setEllipsize(TextUtils.TruncateAt.END);
        alarmTimeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);

        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_VERTICAL);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);

        alarmTimeTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (daydreamMode) return;
                SetAlarmClockActivity.start(getContext());
            }
        });

        alarmClockView.setOnAlarmChangedListener(new AlarmClockView.onAlarmChangeListener() {
            @Override
            public void onAlarmChanged(String alarmString) {
                alarmTimeTextView.setText(alarmString);
                alarmTimeTextView.setVisibility(alarmString.isEmpty() ? GONE : VISIBLE);
                alarmTimeTextView.invalidate();
            }
        });

        addView(alarmClockView, layoutAlarmClockView);
        addView(alarmTimeTextView, lp);
    }

    public void setDaydreamMode(boolean enabled) {
        this.daydreamMode = enabled;
        alarmClockView.setDaydreamMode(enabled);
    }

    public void setLocked(boolean on) {
        alarmClockView.setLocked(on);
    }

    public void setCustomColor(int primary, int secondary) {
        customSecondaryColor = secondary;
        initColorFilters();
        alarmClockView.setCustomColor(primary, secondary);
        alarmTimeTextView.setTextColor(secondary);
        invalidate();
    }

    private void initColorFilters() {
        ColorFilter customColorFilterImage = new PorterDuffColorFilter(
                customSecondaryColor, PorterDuff.Mode.SRC_ATOP);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                && alarmTimeTextView != null) {
            Drawable icon = getResources().getDrawable(R.drawable.ic_alarm_clock);
            icon = icon.mutate();
            icon.setColorFilter(customColorFilterImage);
            alarmTimeTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
            alarmTimeTextView.setCompoundDrawablePadding(Utility.dpToPx(getContext(), 20.f));
        }
    }

    public boolean isInteractive() {
        return alarmClockView.isInteractive();
    }

    @Override
    protected void onDraw(Canvas canvas){
        if ( !isClickable() ) return;

//        if (showAlarmTime()) {
//            String l = getAlarmTimeFormatted();
//            alarmTimeTextView.setTextColor(customSecondaryColor);
//            alarmTimeTextView.setText(l);
//            alarmTimeTextView.setVisibility(VISIBLE);
//        } else {
//            alarmTimeTextView.setVisibility(GONE);
//        }
    }

    @Override
    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
        alarmTimeTextView.setClickable(clickable);
        alarmClockView.setClickable(clickable);

    }

    public void activateAlarmUI() {
        alarmClockView.activateAlarmUI();
    }

    public void snooze() {
        alarmClockView.snooze();
    }

    public void cancelAlarm(){
        alarmClockView.cancelAlarm();
    }
}
