package com.firebirdberlin.nightdream.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.ui.ClockLayout;

public class LayoutTestView extends LinearLayout {

    private static final String TAG = "NightDream.TestView";

    private ClockLayout clockLayout = null;

    public LayoutTestView(Context context) {
        super(context);

    }

    public LayoutTestView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View rootView = inflater.inflate(R.layout.clock_widget_clock_layout, this, false); // important for centering! set this as parent view group

        clockLayout = (ClockLayout) rootView.findViewById(R.id.clockLayout);
        clockLayout.setLayout(ClockLayout.LAYOUT_ID_DIGITAL);
        Configuration config = context.getResources().getConfiguration();
        clockLayout.setScaleFactor(1f);
        //clockLayout.updateLayout(400, config);
        clockLayout.requestLayout();
        clockLayout.invalidate();

        Log.d(TAG, "view width=" + this.getMeasuredWidth());

        this.addView(rootView);

    }

    @Override
    protected void onLayout(boolean changed,
                            int left,
                            int top,
                            int right,
                            int bottom) {

        Log.d(TAG, "onLayout view width=" + this.getWidth());
        if (changed) {
            Configuration config = getContext().getResources().getConfiguration();
            //clockLayout.updateLayout(this.getWidth(), config);

        }

        super.onLayout(changed, left, top, right, bottom);
    }
}
