package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.SetAlarmClockActivity;
import com.firebirdberlin.nightdream.Utility;


public class AlarmClock extends RelativeLayout {
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
        OnClickListener alarmTimeTextVieOnCLickListener = view -> SetAlarmClockActivity.start(getContext());
        alarmTimeTextView.setOnClickListener(alarmTimeTextVieOnCLickListener);

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
