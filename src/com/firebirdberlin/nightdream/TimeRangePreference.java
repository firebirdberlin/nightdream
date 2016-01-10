package com.firebirdberlin.nightdream;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import java.util.Date;


public class TimeRangePreference extends DialogPreference {
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
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

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
    protected View onCreateDialogView() {

        LinearLayout layout = new LinearLayout(mContext);
        LinearLayout verticalLayout1 = new LinearLayout(mContext);

        verticalLayout1.setOrientation(LinearLayout.VERTICAL);

        verticalLayout1.setPadding(12,12,12,12);

        int orientation = mContext.getResources().getConfiguration().orientation;
        layout.setOrientation( ( orientation == Configuration.ORIENTATION_PORTRAIT )
                                ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);

        layout.setGravity(Gravity.CENTER);

        timeLabel = new TextView(mContext);
        timeLabel.setText(label_start_text);
        timeLabel.setGravity(Gravity.CENTER_HORIZONTAL);

        picker = new TimePicker(mContext);

         if (DateFormat.is24HourFormat(mContext) ) {
             picker.setIs24HourView(true);
         }

        picker.setScaleX(0.95f);
        picker.setScaleY(0.95f);

        verticalLayout1.addView(timeLabel);
        verticalLayout1.addView(picker);
        layout.addView(verticalLayout1);

        return layout;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        dialogState = 0;
        final AlertDialog dialog = (AlertDialog) getDialog();

        setPicker(startTime);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (dialogState == 0) {
                            startTime.hour = picker.getCurrentHour();
                            startTime.min = picker.getCurrentMinute();
                            timeLabel.setText(label_end_text);
                            timeLabel.invalidate();
                            setPicker(endTime);

                            dialogState++;
                        } else {
                            endTime.hour = picker.getCurrentHour();
                            endTime.min = picker.getCurrentMinute();
                            dialog.dismiss();
                            onDialogClosed(true);
                        }
                    }
                });
    }

    void setPicker(SimpleTime simpleTime) {
        picker.setCurrentHour(simpleTime.hour);
        picker.setCurrentMinute(simpleTime.min);
        picker.invalidate();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            setSummary(getSummary());
            if (callChangeListener(startTime.getMillis())) {
                settings.setAutoStartTime(startTime.getMillis(), endTime.getMillis());
                notifyChanged();
            }
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
