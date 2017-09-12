package com.firebirdberlin.nightdream.ui;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static android.text.format.DateFormat.getBestDateTimePattern;
import static android.text.format.DateFormat.is24HourFormat;

public class StockAlarmLayout extends RelativeLayout {
    private Context context;
    private TextView textView;
    public boolean locked = false;
    private OnClickListener onStockAlarmTimeClickListener = new OnClickListener() {
        public void onClick(View v) {
            if (Build.VERSION.SDK_INT < 19) return;

            Intent mClockIntent = new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS);
            mClockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mClockIntent);
        }
    };
    public StockAlarmLayout(Context context) {
        super(context);
        this.context = context;
    }

    public StockAlarmLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        textView = new TextView(context);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        textView.setOnClickListener(onStockAlarmTimeClickListener);
        addView(textView, lp);

    }

    public void setCustomColor(int accentColor, int textColor) {
        textView.setTextColor(textColor);
    }

    protected void setText() {
        String nextAlarm = getNextSystemAlarmTime();
        if ( Build.VERSION.SDK_INT >= 19
                && nextAlarm != null
                && nextAlarm.isEmpty()
                && ! locked ) {
            nextAlarm = context.getString(R.string.set_alarm);
        }
        textView.setText(nextAlarm);
    }

    public String getNextSystemAlarmTime() {
        if ( Build.VERSION.SDK_INT < 21 ) {
            return deprecatedGetNextSystemAlarmTime();
        }
        AlarmManager am = (AlarmManager) (context.getSystemService( Context.ALARM_SERVICE ));
        AlarmManager.AlarmClockInfo info = am.getNextAlarmClock();
        if (info != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(info.getTriggerTime());
            return getTimeFormatted(cal);
        }
        return "";
    }

    @SuppressWarnings("deprecation")
    private String deprecatedGetNextSystemAlarmTime() {
        return android.provider.Settings.System.getString(
                context.getContentResolver(),
                android.provider.Settings.System.NEXT_ALARM_FORMATTED);
    }

    private String getTimeFormatted(Calendar calendar) {
        String localPattern  = "";
        if (Build.VERSION.SDK_INT >= 18){
            if (is24HourFormat(context)) {
                localPattern = getBestDateTimePattern(Locale.getDefault(), "EE HH:mm");
            } else {
                localPattern = getBestDateTimePattern(Locale.getDefault(), "EE hh:mm a");
            }
        } else {
            DateFormat formatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
            localPattern  = ((SimpleDateFormat)formatter).toLocalizedPattern();
        }

        SimpleDateFormat hourDateFormat = new SimpleDateFormat(localPattern, Locale.getDefault());
        return hourDateFormat.format(calendar.getTime());
    }

    @Override
    public void setClickable(boolean clickable) {
        Log.w("StockAlarmLayout", "setClickable " + (clickable ? "true" : "false"));
        super.setClickable(clickable);
        textView.setOnClickListener(clickable ? onStockAlarmTimeClickListener : null);
    }

}
