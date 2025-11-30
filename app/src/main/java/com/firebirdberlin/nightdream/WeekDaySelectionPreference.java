/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
