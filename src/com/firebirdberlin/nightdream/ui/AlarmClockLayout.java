package com.firebirdberlin.nightdream.ui;


import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.models.SimpleTime;

public class AlarmClockLayout extends LinearLayout {

    private static final String TAG = "AlarmClockLayout";
    private Context context = null;

    private SimpleTime alarmClockEntry = null;
    private TextView timeView = null;
    private ImageView buttonDown = null;
    private ImageButton buttonDelete = null;
    private Switch switchActive = null;
    private RelativeLayout middle = null;

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
        buttonDelete.setTag(entry);
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
        buttonDown = (ImageView) findViewById(R.id.button_down);
        buttonDelete = (ImageButton) findViewById(R.id.button_delete);
        switchActive = (Switch) findViewById(R.id.enabled);
        middle = (RelativeLayout) findViewById(R.id.middle);
        middle.setPivotY(0.f);
        if (alarmClockEntry != null) {
            timeView.setText(alarmClockEntry.toString());
            switchActive.setChecked(alarmClockEntry.isActive);
        }

        buttonDown.setSoundEffectsEnabled(false);
        buttonDown.setOnClickListener(new ImageView.OnClickListener() {
            @Override
            public void onClick(View view) {
                int visibility = buttonDelete.getVisibility();
                buttonDelete.setVisibility((visibility == View.GONE) ? View.VISIBLE : View.GONE);
                /*
                if (visibility == View.VISIBLE) {

                    //middle.animate().scaleY(0.5f).setDuration(3500);
                    middle.animate()
                            .yBy(-buttonDelete.getHeight())
                            //.alpha(0.0f)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    buttonDelete.setVisibility(View.INVISIBLE);
                                }
                            });
                } else {
                    //middle.animate().scaleY(1.f).setDuration(3500);
                    buttonDelete.setVisibility(View.VISIBLE);
                    middle.animate().yBy(buttonDelete.getHeight()).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
//                            buttonDelete.animate().alpha(1.f);
                        }
                    });
                }
                */
            }
        });
    }
}
