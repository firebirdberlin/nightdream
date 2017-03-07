package com.firebirdberlin.nightdream;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.Preference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import java.util.Date;

import com.firebirdberlin.nightdream.models.SimpleTime;


public class TimeRangePreference extends Preference {
    private static final String TIMERANGE = "timerange";

    private int dialogState = 0;
    private Context mContext = null;
    private String key_suffix_end = "";
    private String key_suffix_start = "";
    private String label_end_text = "";
    private String label_start_text = "";
    private TimePicker picker = null;
    private Settings settings = null;
    private SimpleTime startTime = new SimpleTime();
    private SimpleTime endTime = new SimpleTime();
    private TextView timeLabel = null;
    private LinearLayout verticalLayout1 = null;


    public TimeRangePreference(Context ctxt) {
        this(ctxt, null);
        mContext = getContext();
    }

    public TimeRangePreference(Context ctxt, AttributeSet attrs) {
        this(ctxt, attrs, android.R.attr.dialogPreferenceStyle);
        mContext = getContext();
        setValuesFromXml(attrs);
    }

    public TimeRangePreference(Context ctxt, AttributeSet attrs, int defStyle) {
        super(ctxt, attrs, defStyle);
        setValuesFromXml(attrs);
    }

    private void setValuesFromXml(AttributeSet attrs) {
        mContext = getContext();
        settings = new Settings(mContext);

        label_start_text = mContext.getResources().getString(R.string.autostart_timerange_label_start);
        label_end_text = mContext.getResources().getString(R.string.autostart_timerange_label_end);
        key_suffix_start = getAttributeStringValue(attrs, TIMERANGE, "key_suffix_start", "_start");
        key_suffix_end = getAttributeStringValue(attrs, TIMERANGE, "key_suffix_end", "_end");
    }

    private static String getAttributeStringValue(AttributeSet attrs, String namespace,
                                                  String name, String defaultValue) {
        String value = attrs.getAttributeValue(namespace, name);
        if (value == null) value = defaultValue;

        return value;
    }

    @Override
    public void onClick() {
            TimePickerDialog mTimePicker;
            mTimePicker = new TimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                    updateTime(startTime, selectedHour, selectedMinute);
                    showDialogEndTime();

                }
            }, startTime.hour, startTime.min, DateFormat.is24HourFormat(mContext));
            setDialogTitle(mTimePicker, label_start_text);
            mTimePicker.show();
    }

    public void showDialogEndTime() {
            TimePickerDialog mTimePicker;
            mTimePicker = new TimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                    updateTime(endTime, selectedHour, selectedMinute);
                }
            }, endTime.hour, endTime.min, DateFormat.is24HourFormat(mContext));
            setDialogTitle(mTimePicker, label_end_text);
            mTimePicker.show();
    }

    private void setDialogTitle(TimePickerDialog dialog, String title) {
        /* Due to a bug in the TimePicker view we set the title to an empty string
         * if the screen orientation is landscape
         * https://code.google.com/p/android/issues/detail?id=201766
         */
        int orientation = mContext.getResources().getConfiguration().orientation;
        if (Build.VERSION.SDK_INT > 19
                && orientation == Configuration.ORIENTATION_LANDSCAPE ) {
            dialog.setTitle("");
        } else {
            dialog.setTitle(title);
        }
    }

    private void updateTime(SimpleTime time, int hour, int min) {
        time.hour = hour;
        time.min = min;
        setSummary(getSummary());
        if (callChangeListener(time.getMillis())) {
            settings.setAutoStartTime(startTime.getMillis(), endTime.getMillis());
            notifyChanged();
        }
    }
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        long startInMillis = settings.autostartTimeRangeStart;
        long endInMillis = settings.autostartTimeRangeEnd;

        startTime = new SimpleTime(startInMillis);
        endTime = new SimpleTime(endInMillis);

        setTitle(getTitle());
    }

    @Override
    public CharSequence getTitle() {
        if (startTime == null || endTime == null) {
            return null;
        }

        long start = startTime.getCalendar().getTimeInMillis();
        long end = endTime.getCalendar().getTimeInMillis();

        return DateFormat.getTimeFormat(mContext).format(new Date(start)) + " -- " +
               DateFormat.getTimeFormat(mContext).format(new Date(end));
    }

}
