package com.firebirdberlin.nightdream.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.SetAlarmClockActivity;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.repositories.VibrationHandler;
import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.SqliteIntentService;


public class AlarmClock extends RelativeLayout {
    static String TAG = "AlarmClock";
    private final TextView alarmTimeTextView;
    protected AlarmClockView alarmClockView;
    private int customSecondaryColor = Color.parseColor("#C2C2C2");
    private boolean hasUserInteraction = false;

    @SuppressLint("ClickableViewAccessibility")
    public AlarmClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClipChildren(false);
        initColorFilters();

        alarmClockView = new AlarmClockView(context, attrs);
        LayoutParams layoutAlarmClockView = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        alarmTimeTextView = new TextView(context);
        alarmTimeTextView.setEllipsize(TextUtils.TruncateAt.END);
        alarmTimeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        alarmTimeTextView.setOnTouchListener(new OnTouchListener() {
            final Handler handler = new Handler();
            final Runnable longPress = () -> {
                VibrationHandler vibrationHandler = new VibrationHandler(context);
                vibrationHandler.startOneShotVibration(50);
            };
            float startX = 0;
            float max_move_seen = 0;
            long timestampDown = 0;
            final float threshold = 0.25f;
            SimpleTime nextEventTime = AlarmHandlerService.getCurrentlyActiveAlarm();

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (AlarmHandlerService.alarmIsRunning() || !isClickable()) {
                    return false;
                }
                int action = motionEvent.getAction();
                float x = motionEvent.getX();
                float rawX = motionEvent.getRawX();
                long now = System.currentTimeMillis();

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "DOWN:" + x);
                        hasUserInteraction = true;
                        timestampDown = now;
                        startX = -1;
                        max_move_seen = 0;
                        handler.postDelayed(longPress, 500);

                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.d(TAG, "MOVE:" + x);
                        if (now - timestampDown > 500) {
                            if (startX < 0) {
                                startX = rawX;
                            }
                            float diff_x = rawX - startX;
                            if (diff_x > 0.) {
                                alarmTimeTextView.setPadding((int) diff_x, 0, 0, 0);
                                float alpha = diff_x / (threshold * alarmTimeTextView.getWidth());
                                alarmTimeTextView.setAlpha(1.f - alpha);
                            }
                            if (diff_x > max_move_seen) {
                                max_move_seen = diff_x;
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(longPress);
                        Log.d(TAG, "UP:" + x);
                        alarmTimeTextView.setAlpha(1.f);
                        alarmTimeTextView.setPadding(0, 0, 0, 0);
                        if (
                                now - timestampDown > 500
                                        && rawX - startX > threshold * alarmTimeTextView.getWidth()
                        ) {
                            //skip next alarm
                            nextEventTime = alarmClockView.getCurrentlyActiveAlarm();
                            if (nextEventTime != null) {
                                Intent i = AlarmHandlerService.getSkipIntent(context, nextEventTime);
                                getContext().startService(i);

                                SqliteIntentService.scheduleAlarm(context);
                            }
                        } else if (
                                now - timestampDown < 300
                                        && max_move_seen < 0.1f * alarmTimeTextView.getWidth()
                        ) {
                            // treat as click
                            SetAlarmClockActivity.start(getContext());
                        }
                        hasUserInteraction = false;
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        Log.d(TAG, "CANCEL:" + x);
                        alarmTimeTextView.setAlpha(1.f);
                        alarmTimeTextView.setPadding(0, 0, 0, 0);
                        hasUserInteraction = false;
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
        return hasUserInteraction || alarmClockView.isInteractive();
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
