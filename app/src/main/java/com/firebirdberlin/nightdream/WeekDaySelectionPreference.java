package com.firebirdberlin.nightdream;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.preference.MultiSelectListPreference;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class WeekDaySelectionPreference extends MultiSelectListPreference {

    public WeekDaySelectionPreference(Context context) {
        super(context);
        init();
    }

    public WeekDaySelectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WeekDaySelectionPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public WeekDaySelectionPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        String[] weekdays = getWeekdayStrings();
        ArrayList<String> entries = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        int firstDay = cal.getFirstDayOfWeek();
        for (int i = firstDay; i < firstDay + 8; i++) {
            if (i % 8 == 0) continue;
            Log.d("TAG", i + " " + (i % 8) + " "+ weekdays[i % 8]);
            entries.add(weekdays[i % 8]);
            values.add(String.valueOf(i % 8));
        }
        setEntries(entries.toArray(new String[0]));
        setEntryValues(values.toArray(new String[0]));
        Set<String> defaults = new HashSet<>(values);
        setDefaultValue(defaults);
    }

    private static String[] getWeekdayStrings() {
        return getWeekdayStringsForLocale(Locale.getDefault());
    }

    private static String[] getWeekdayStringsForLocale(Locale locale) {
        DateFormatSymbols symbols = new DateFormatSymbols(locale);
        return symbols.getWeekdays();
    }
}
