package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import com.firebirdberlin.nightdream.services.RadioStreamService;

public class BottomPanelLayout extends FrameLayout {
    public boolean isVisible = false;
    public boolean useInternalAlarm = false;
    private Context context;
    private AttributeSet attrs;
    private StockAlarmLayout stockAlarmView = null;
    private WebRadioLayout webRadioLayout = null;
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
        if (RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO) return;
        
        removeAllViews();
        stockAlarmView = new StockAlarmLayout(context, attrs);
        stockAlarmView.setCustomColor(accentColor, textColor);
        stockAlarmView.setText();
        addView(stockAlarmView);
        invalidate();
    }

    public void showWebRadioView() {
        removeAllViews();
        webRadioLayout = new WebRadioLayout(context, attrs);
        webRadioLayout.setCustomColor(accentColor, textColor);
        webRadioLayout.setText();
        addView(webRadioLayout);
        invalidate();
    }

    public void showAlarmView() {
        if (RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO) return;

        removeAllViews();
        stockAlarmView = null;
        addView(view);
        invalidate();
    }

    public void setup() {
        if (RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO){
            showWebRadioView();
        } else {


        }
    }

    public void setLocked(boolean locked) {
        view.setLocked(locked);
        if (stockAlarmView != null) stockAlarmView.setLocked(locked);
        if (webRadioLayout != null) webRadioLayout.setLocked(locked);
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

