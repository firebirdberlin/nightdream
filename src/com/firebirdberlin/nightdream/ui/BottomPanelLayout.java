package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

public class BottomPanelLayout extends FrameLayout {
    public boolean isVisible = false;
    private Context context;
    private AttributeSet attrs;
    private StockAlarmLayout stockAlarmView = null;
    private int accentColor;
    private int textColor;
    private AlarmClock view = null;

    public BottomPanelLayout(Context context) {
        super(context);
        this.context = context;

    }

    public BottomPanelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.attrs = attrs;
        view = new AlarmClock(context, attrs);
        addView(view);

    }

    public void setCustomColor(int accentColor, int textColor) {
        this.accentColor = accentColor;
        this.textColor = textColor;
        view.setCustomColor(accentColor, textColor);
    }

    public void showStockAlarmView() {
        removeAllViews();
        stockAlarmView = new StockAlarmLayout(context, attrs);
        stockAlarmView.setCustomColor(accentColor, textColor);
        stockAlarmView.setText();
        addView(stockAlarmView);
        invalidate();
    }

    public void showAlarmView() {
        removeAllViews();
        stockAlarmView = null;
        addView(view);
        invalidate();
    }

    public void setLocked(boolean locked) {
        view.setLocked(locked);
        if (stockAlarmView != null) stockAlarmView.setLocked(locked);
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

