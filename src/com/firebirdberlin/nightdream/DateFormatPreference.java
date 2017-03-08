package com.firebirdberlin.nightdream;

import java.lang.String;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.os.Build;
import android.preference.ListPreference;
import android.util.AttributeSet;
import static android.text.format.DateFormat.getBestDateTimePattern;


public class DateFormatPreference extends ListPreference {

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
        ArrayList<CharSequence> entryList = new ArrayList<CharSequence>();
        HashSet<CharSequence> valueList = new HashSet<CharSequence>();

        for (int type: types ) {
            String format = getFormatString(type);
            valueList.add(format);
        }

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
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

}
