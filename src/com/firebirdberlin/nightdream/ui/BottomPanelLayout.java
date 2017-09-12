package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public class BottomPanelLayout extends FrameLayout {
    private Context context;
    private StockAlarmLayout stockAlarmView = null;
    private AlarmClock view = null;
    public boolean isVisible = false;

    public BottomPanelLayout(Context context) {
        super(context);
        this.context = context;

    }

    public BottomPanelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        stockAlarmView = new StockAlarmLayout(context, attrs);
        view = new AlarmClock(context, attrs);
        addView(view);

    }

    public void setCustomColor(int accentColor, int textColor) {
        view.setCustomColor(accentColor, textColor);
        stockAlarmView.setCustomColor(accentColor, textColor);
    }

    public void showStockAlarmView() {
        removeAllViews();
        stockAlarmView.setText();
        addView(stockAlarmView);
        invalidate();
    }

    public void showAlarmView() {
        removeAllViews();
        addView(view);
        invalidate();
    }

    public AlarmClock getAlarmClock() {
        return view;
    }

    @Override
    public void setClickable(boolean clickable) {
        Log.w("BottomPanelLayout", "setClickable " + ((clickable) ? "true" : "false"));
        super.setClickable(clickable);
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setClickable(clickable);
        }
    }

}

