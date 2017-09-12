package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class BottomPanelLayout extends FrameLayout {
    private Context context;
    private View view = null;
    public boolean isVisible = false;

    public BottomPanelLayout(Context context) {
        super(context);
        this.context = context;

    }

    public BottomPanelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        view = new AlarmClock(context, attrs);
        addView(view);

    }

    public AlarmClock getAlarmClock() {
        return (AlarmClock) view;
    }

}
