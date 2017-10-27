package com.firebirdberlin.nightdream.ui;


import android.content.Context;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.SetAlarmClockActivity;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.SimpleTime;

import java.util.Calendar;

public class AlarmClockLayout extends LinearLayout {

    private static final String TAG = "AlarmClockLayout";
    private Context context = null;
    private String timeFormat = "h:mm";

    private SimpleTime alarmClockEntry = null;
    private TextView timeView = null;
    private TextView textViewWhen = null;
    private ImageView buttonDown = null;
    private LinearLayout layoutDays = null;
    private Button buttonDelete = null;
    private Switch switchActive = null;
    private RelativeLayout middle = null;

    public AlarmClockLayout(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public AlarmClockLayout(Context context, SimpleTime entry, String timeFormat) {
        super(context);
        this.context = context;
        this.alarmClockEntry = entry;
        this.timeFormat = timeFormat;
        init();
        buttonDelete.setTag(entry);
        timeView.setTag(entry);
    }

    public AlarmClockLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public static boolean isTomorrow(Calendar d) {
        return DateUtils.isToday(d.getTimeInMillis() - DateUtils.DAY_IN_MILLIS);
    }

    public static boolean isToday(Calendar d) {
        return DateUtils.isToday(d.getTimeInMillis());
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child = inflater.inflate(R.layout.alarm_clock_layout, null);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        addView(child, lp);

        timeView = (TextView) findViewById(R.id.timeView);
        textViewWhen = (TextView) findViewById(R.id.textViewWhen);
        layoutDays = (LinearLayout) findViewById(R.id.layout_days);
        buttonDown = (ImageView) findViewById(R.id.button_down);
        buttonDelete = (Button) findViewById(R.id.button_delete);
        switchActive = (Switch) findViewById(R.id.enabled);
        middle = (RelativeLayout) findViewById(R.id.middle);

        buttonDown.setSoundEffectsEnabled(false);
        buttonDown.setOnClickListener(new ImageView.OnClickListener() {
            @Override
            public void onClick(View view) {
                int visibility = buttonDelete.getVisibility();
                buttonDelete.setVisibility((visibility == View.GONE) ? View.VISIBLE : View.GONE);
                layoutDays.setVisibility((visibility == View.GONE) ? View.VISIBLE : View.GONE);
            }
        });

        update();
        switchActive.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                alarmClockEntry.isActive = isChecked;
                ((SetAlarmClockActivity) context).onActiveStateChanged(alarmClockEntry);
            }
        });

    }

    private void update() {
        if (alarmClockEntry != null) {
            Calendar time = alarmClockEntry.getCalendar();
            String text = Utility.formatTime(timeFormat, time);
            timeView.setText(text);
            switchActive.setChecked(alarmClockEntry.isActive);

            if (isToday(time)) {
                textViewWhen.setText(R.string.today);
            } else if (isTomorrow(time)) {
                textViewWhen.setText(R.string.tomorrow);
            } else {
                textViewWhen.setText(Utility.formatTime("EEEE", time));
            }
        }
    }

}
