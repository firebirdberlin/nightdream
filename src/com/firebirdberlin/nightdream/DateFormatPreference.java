package com.firebirdberlin.nightdream;

import java.lang.String;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.preference.ListPreference;
import android.util.AttributeSet;
import static android.text.format.DateFormat.getBestDateTimePattern;


public class DateFormatPreference extends ListPreference {
    private static final String FORMAT_TYPE_DATE = "date";
    private static final String FORMAT_TYPE_HOUR = "time";

    private String formatType = FORMAT_TYPE_DATE;
    private FormatChangeObserver mFormatChangeObserver;
    static private List<Integer> types = Arrays.asList(DateFormat.FULL,
                                                       DateFormat.LONG,
                                                       DateFormat.MEDIUM,
                                                       DateFormat.SHORT);
    static private List<String> skeletons = Arrays.asList("ddMM",
                                                          "ddMMM",
                                                          "ddMMMM",
                                                          "ddMMMMyy",
                                                          "ddMMMMyyyy",
                                                          "ddMMMyy",
                                                          "ddMMMyyyy",
                                                          "ddMMyy",
                                                          "ddMMyyyy",
                                                          "dMM",
                                                          "dMMyy",
                                                          "dMMM",
                                                          "dMMMyy",
                                                          "dMMMyyyy",
                                                          "EEddMM",
                                                          "EEddMMM",
                                                          "EEddMMMyy",
                                                          "EEddMMMyyyy",
                                                          "EEddMMMM",
                                                          "EEddMMMMyy",
                                                          "EEddMMMMyyyy",
                                                          "EEddMMyy",
                                                          "EEddMMyyyy",
                                                          "EEEddMMMyyyy",
                                                          "EEEdMMMyy",
                                                          "EEEdMMMyyyy",
                                                          "EEEdMMyy",
                                                          "EEEdMMyyyy",
                                                          "EEEEddMM",
                                                          "EEEEddMMMM",
                                                          "EEEEddMMMMyy",
                                                          "EEEEddMMMMyyyy",
                                                          "EEEEddMMyy",
                                                          "EEEEddMMyyyy");

    public DateFormatPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DateFormatPreference);
        formatType = a.getString(R.styleable.DateFormatPreference_formatType);
        a.recycle();

        if ( FORMAT_TYPE_HOUR.equals(formatType) ) {
            mFormatChangeObserver = new FormatChangeObserver();
            context.getContentResolver().registerContentObserver(
                android.provider.Settings.System.CONTENT_URI, true, mFormatChangeObserver);
            initHourFormatType();
        } else {
            initDateFormatType();
        }
    }

    private void initHourFormatType() {
        ArrayList<CharSequence> entryList = new ArrayList<CharSequence>();
        HashSet<CharSequence> valueList = new HashSet<CharSequence>();
        boolean is24Hour = is24HourFormat();
        if ( is24Hour ) {
            //valueList.add("k:mm");
            //valueList.add("kk:mm");
            valueList.add("H:mm");
            valueList.add("HH:mm");
        } else {
            valueList.add("h:mm");
            valueList.add("hh:mm");
            //valueList.add("K:mm");
            //valueList.add("KK:mm");
        }

        CharSequence[] values = valueList.toArray(new CharSequence[valueList.size()]);
        Arrays.sort(values);

        Calendar cal = Calendar.getInstance();
        Date date1 = setHour(cal, 9);
        Date date2 = setHour(cal, 12);
        Date date3 = setHour(cal, 0);

        for (CharSequence value : values) {
            if ( !is24Hour ) value += " a";
            String example = String.format("%s / %s / %s",
                                           dateAsString(value.toString(), date1),
                                           dateAsString(value.toString(), date2),
                                           dateAsString(value.toString(), date3)
                                           );
            entryList.add(example);

        }
        CharSequence[] cs = entryList.toArray(new CharSequence[entryList.size()]);

        setEntries(cs);
        setEntryValues(values);
    }

    private Date setHour(Calendar cal, int hour) {
        cal.set(Calendar.HOUR_OF_DAY, hour);
        return cal.getTime();
    }

    private boolean is24HourFormat() {
        return android.text.format.DateFormat.is24HourFormat(getContext());
    }

    private void initDateFormatType() {
        ArrayList<CharSequence> entryList = new ArrayList<CharSequence>();
        HashSet<CharSequence> valueList = new HashSet<CharSequence>();

        // load the default date formats
        for (int type: types ) {
            String format = getFormatString(type);
            valueList.add(format);
        }

        // for newer api levels add custom formats
        if (Build.VERSION.SDK_INT >= 18){
            for (String value : skeletons) {
                String format = getDateFormat(value);
                valueList.add(format);
            }
        }

        CharSequence[] values = valueList.toArray(new CharSequence[valueList.size()]);
        Arrays.sort(values);
        for (CharSequence value : values) {
            entryList.add(dateAsString(value.toString()));

        }
        CharSequence[] cs = entryList.toArray(new CharSequence[entryList.size()]);

        setEntries(cs);
        setEntryValues(values);
    }

    private String getFormatString(int type) {
        DateFormat formatter = DateFormat.getDateInstance(type, Locale.getDefault());
        return ((SimpleDateFormat) formatter).toLocalizedPattern();
    }

    // only for api level >= 18
    private String getDateFormat(String skeleton) {
        if (Build.VERSION.SDK_INT < 18){
            return "";
        }
        return getBestDateTimePattern(Locale.getDefault(), skeleton);
    }

    private String dateAsString(String format) {
        Date date = new Date();
        return dateAsString(format, date);
    }

    private String dateAsString(String format, Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    private class FormatChangeObserver extends ContentObserver {
        public FormatChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            if (FORMAT_TYPE_HOUR.equals(formatType) ) {
                initHourFormatType();
            }
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult && FORMAT_TYPE_HOUR.equals(formatType)) {
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.putString(getTimeKey(), getValue());
            editor.commit();
        }
    }

    public String getTimeKey() {
        if (FORMAT_TYPE_HOUR.equals(formatType)) {
            return String.format("%s_%s",
                                 super.getKey(),
                                 (is24HourFormat()) ? "24h" : "12h");
        }

        return super.getKey();
    }
}
