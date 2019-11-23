package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.format.DateFormat;
import android.util.AttributeSet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import java.util.Calendar;
import java.util.Date;


public class TimeRangePreference
        extends Preference
        implements TimeRangePickerDialogFragment.Result {

    private static final String TIMERANGE = "timerange";

    class SimpleTime {

        public int hour = 0;
        public int min = 0;

        SimpleTime(long millis){
            if (millis < 0L) return;
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(millis);
            hour = calendar.get(Calendar.HOUR_OF_DAY);
            min = calendar.get(Calendar.MINUTE);
        }

        SimpleTime(int minutes){
            this.hour = minutes / 60;
            this.min = minutes % 60;
        }

        int toMinutes() {
            return this.hour * 60 + this.min;
        }

        long getMillis() {
            return getCalendar().getTimeInMillis();
        }

        public Calendar getCalendar() {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, min);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            Calendar now = Calendar.getInstance();
            if ( cal.before(now) ) {
                cal.add(Calendar.DATE, 1);
            }

            return cal;
        }
    }

    private Context mContext;
    private SimpleTime startTime;
    private SimpleTime endTime;
    private String keyStart;
    private String keyEnd;
    private String keyStartInMinutes;
    private String keyEndInMinutes;


    public TimeRangePreference(Context context) {
        super(context, null);
        mContext = context;

    }

    public TimeRangePreference(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.dialogPreferenceStyle);
        mContext = context;
        setValuesFromXml(attrs);
    }

    public TimeRangePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        setValuesFromXml(attrs);
    }

    private void setValuesFromXml(AttributeSet attrs) {
        String key_suffix_start = getAttributeStringValue(attrs, TIMERANGE, "key_suffix_start", "_start");
        String key_suffix_end = getAttributeStringValue(attrs, TIMERANGE, "key_suffix_end", "_end");

        keyStart = getKey() + key_suffix_start;
        keyEnd = getKey() + key_suffix_end;

        keyStartInMinutes = getKey() + key_suffix_start + "_minutes";
        keyEndInMinutes = getKey() + key_suffix_end + "_minutes";
        setIconSpaceReserved(false);
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        setSummary(getSummary());
    }

    private static String getAttributeStringValue(AttributeSet attrs, String namespace,
                                                  String name, String defaultValue) {
        String value = attrs.getAttributeValue(namespace, name);
        if (value == null) value = defaultValue;

        return value;
    }

    @Override
    public void onClick() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
           showTimeRangePickerDialog();
        }
    }

    private void showTimeRangePickerDialog() {
        FragmentManager fm = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        TimeRangePickerDialogFragment dialog = new TimeRangePickerDialogFragment(this);
        dialog.show(fm, "time_range_picker");
        if (startTime != null) {
            dialog.setStartTime(startTime.hour, startTime.min);
        }
        if (endTime != null) {
            dialog.setEndTime(endTime.hour, endTime.min);
        }
    }

    @Override
    public void onTimeRangeSet(int hourStart, int minuteStart, int hourEnd, int minuteEnd) {
        updateTime(startTime, hourStart, minuteStart);
        updateTime(endTime, hourEnd, minuteEnd);
    }

    private void updateTime(SimpleTime time, int hour, int min) {
        time.hour = hour;
        time.min = min;
        setSummary(getSummary());
        if (callChangeListener(time.getMillis())) {
            SharedPreferences sharedPreferences = getSharedPreferences();
            SharedPreferences.Editor prefEditor = sharedPreferences.edit();
            prefEditor.putInt(keyStartInMinutes, startTime.toMinutes());
            prefEditor.putInt(keyEndInMinutes, endTime.toMinutes());
            prefEditor.apply();
            notifyChanged();
        }
    }
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        SharedPreferences sharedPreferences = getSharedPreferences();
        int startInMinutes = sharedPreferences.getInt(keyStartInMinutes, -1);
        int endInMinutes = sharedPreferences.getInt(keyEndInMinutes, -1);

        if (startInMinutes > -1 && endInMinutes > -1) {
            startTime = new SimpleTime(startInMinutes);
            endTime = new SimpleTime(endInMinutes);
        } else {
            long startInMillis = sharedPreferences.getLong(keyStart, -1L);
            long endInMillis = sharedPreferences.getLong(keyEnd, -1L);

            startTime = new SimpleTime(startInMillis);
            endTime = new SimpleTime(endInMillis);
        }
        setSummary(getSummary());
    }

    @Override
    public CharSequence getSummary() {
        if (startTime == null || endTime == null) {
            return null;
        }

        long start = startTime.getCalendar().getTimeInMillis();
        long end = endTime.getCalendar().getTimeInMillis();

        return DateFormat.getTimeFormat(mContext).format(new Date(start)) + " -- " +
               DateFormat.getTimeFormat(mContext).format(new Date(end));
    }

}
