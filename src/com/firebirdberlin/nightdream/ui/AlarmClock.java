package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Log;
import android.view.MotionEvent; 
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.DataSource;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.SetAlarmClockActivity;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.SqliteIntentService;


public class AlarmClock extends RelativeLayout {
    static String TAG = "AlarmClock";
    private final TextView alarmTimeTextView;
    protected AlarmClockView alarmClockView;
    private int customSecondaryColor = Color.parseColor("#C2C2C2");

    public AlarmClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClipChildren(false);
        initColorFilters();

        alarmClockView = new AlarmClockView(context, attrs);
        LayoutParams layoutAlarmClockView = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        alarmTimeTextView = new TextView(context);
        alarmTimeTextView.setEllipsize(TextUtils.TruncateAt.END);
        alarmTimeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        OnClickListener alarmTimeTextViewOnCLickListener = view -> SetAlarmClockActivity.start(getContext());
        //alarmTimeTextView.setOnClickListener(alarmTimeTextViewOnCLickListener);
        alarmTimeTextView.setOnTouchListener(new OnTouchListener() {
            float startX = 0;
            float text_x = 0;
            float max_move_seen = 0;
            SimpleTime nextEventTime = AlarmHandlerService.getCurrentlyActiveAlarm();;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                float x = motionEvent.getX();
                float X = motionEvent.getRawX();
                float y = motionEvent.getY();
                switch(action) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "DOWN:" + x);
                        startX = X;
                        max_move_seen = 0;
                        text_x = alarmTimeTextView.getX();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float diff_x = X - startX;
                        Log.d(TAG, "MOVE:" + x);
                        if (diff_x > 0.) {
                            alarmTimeTextView.setPadding((int) diff_x, 0, 0, 0);
                            float alpha = diff_x / (0.5f * alarmTimeTextView.getWidth());
                            alarmTimeTextView.setAlpha(1.f - alpha);
                        }
                        if (diff_x > max_move_seen) {
                            max_move_seen = diff_x;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.d(TAG, "UP:" + x);
                        alarmTimeTextView.setAlpha(1.f);
                        alarmTimeTextView.setPadding(0, 0, 0, 0);
                        if (X - startX > 0.5*alarmTimeTextView.getWidth()) {
                            //skip next alarm
                            nextEventTime = alarmClockView.getCurrentlyActiveAlarm();

                            Intent i = AlarmHandlerService.getSkipIntent(context,nextEventTime);
                            getContext().startService(i);

                            SqliteIntentService.scheduleAlarm(context);
                        } else if (max_move_seen < 0.2f * alarmTimeTextView.getWidth()) {
                            // treat as click
                            SetAlarmClockActivity.start(getContext());
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        Log.d(TAG, "CANCEL:" + x);
                        alarmTimeTextView.setAlpha(1.f);
                        alarmTimeTextView.setPadding(0, 0, 0, 0);
                        break;
                }
                return false;
            }
        });
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_VERTICAL);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);

        alarmClockView.setOnAlarmChangedListener(alarmString -> {
            alarmTimeTextView.setText(alarmString);
            alarmTimeTextView.setVisibility(alarmString.isEmpty() ? GONE : VISIBLE);
            alarmTimeTextView.invalidate();
        });

        addView(alarmClockView, layoutAlarmClockView);
        addView(alarmTimeTextView, lp);
    }

    public void setLocked(boolean on) {
        alarmClockView.setLocked(on);
    }

    public void setCustomColor(int primary, int secondary) {
        customSecondaryColor = secondary;
        initColorFilters();
        alarmClockView.setCustomColor(primary);
        ColorStateList colorStateList = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_pressed},
                        new int[]{} // default
                },
                new int[]{primary, secondary}
        );

        alarmTimeTextView.setTextColor(colorStateList);
        invalidate();
    }

    private void initColorFilters() {
        ColorFilter customColorFilterImage = new PorterDuffColorFilter(
                customSecondaryColor, PorterDuff.Mode.SRC_ATOP);

        if (alarmTimeTextView != null) {
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

    public void cancelAlarm() {
        alarmClockView.cancelAlarm();
    }

    public void setPaddingHorizontal(int paddingHorizontal) {
        alarmClockView.setPaddingHorizontal(paddingHorizontal);
    }
}
