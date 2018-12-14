package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.preference.ListPreference;
import android.util.AttributeSet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

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
        valueList.add("H:mm");
        valueList.add("HH:mm");
        valueList.add("h:mm");
        valueList.add("hh:mm");

        CharSequence[] values = valueList.toArray(new CharSequence[valueList.size()]);
        Arrays.sort(values);

        Calendar cal = Calendar.getInstance();
        Date date1 = setHour(cal, 9);
        Date date2 = setHour(cal, 12);
        Date date3 = setHour(cal, 0);

        for (CharSequence value : values) {
            String strValue = value.toString();
            if (strValue.startsWith("h")) strValue += " a";
            String example = String.format("%s / %s / %s",
                    dateAsString(strValue, date1),
                    dateAsString(strValue, date2),
                    dateAsString(strValue, date3)
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
    }
}
