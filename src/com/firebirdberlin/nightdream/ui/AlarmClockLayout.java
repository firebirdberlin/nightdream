package com.firebirdberlin.nightdream.ui;


import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.models.SimpleTime;

public class AlarmClockLayout extends LinearLayout {

    private static final String TAG = "AlarmClockLayout";
    private Context context = null;

    private SimpleTime alarmClockEntry = null;
    private TextView timeView = null;
    private Switch switchActive = null;

    public AlarmClockLayout(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public AlarmClockLayout(Context context, SimpleTime entry) {
        super(context);
        this.context = context;
        this.alarmClockEntry = entry;
        init();
    }

    public AlarmClockLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child = inflater.inflate(R.layout.alarm_clock_layout, null);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        addView(child, lp);

        timeView = (TextView) findViewById(R.id.timeView);
        switchActive = (Switch) findViewById(R.id.enabled);
        if (alarmClockEntry != null) {
            timeView.setText(alarmClockEntry.toString());
            switchActive.setEnabled(alarmClockEntry.isActive);


        }


    }
}
