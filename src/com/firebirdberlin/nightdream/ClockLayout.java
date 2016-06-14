package com.firebirdberlin.nightdream;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;


public class ClockLayout extends LinearLayout {

    public ClockLayout(Context context) {
        super(context);
    }

    public ClockLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        return true;
    }

}
