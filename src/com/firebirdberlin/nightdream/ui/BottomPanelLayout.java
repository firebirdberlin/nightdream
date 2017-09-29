package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.RadioStreamService;

public class BottomPanelLayout extends FrameLayout {
    public boolean isVisible = true;
    public boolean useInternalAlarm = false;
    public boolean daydreamMode = false;
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
    }

    public void hide() {
        isVisible = false;
        setClickable(false);
    }

    public void show() {
        isVisible = true;
        setClickable(true);
    }

    public void setCustomColor(int accentColor, int textColor) {
        this.accentColor = accentColor;
        this.textColor = textColor;
        view.setCustomColor(accentColor, textColor);
        if (stockAlarmView != null) stockAlarmView.setCustomColor(accentColor, textColor);
        if (webRadioLayout != null) webRadioLayout.setCustomColor(accentColor, textColor);
    }

    public void setup() {
        if (AlarmHandlerService.alarmIsRunning()) {
            showAlarmView();
        } else if (RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO & !daydreamMode) {
            showWebRadioView();
        } else if (!useInternalAlarm) {
            showStockAlarmView();
        } else {
            showAlarmView();
        }
        show();
        invalidate();
    }

    private void clearViews() {
        stockAlarmView = null;
        webRadioLayout = null;
    }

    private void showStockAlarmView() {
        if (stockAlarmView != null) return; // already visible
        if (RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO) return;
        
        removeAllViews();
        clearViews();
        view.cancelAlarm();
        stockAlarmView = new StockAlarmLayout(context, attrs);
        stockAlarmView.setCustomColor(accentColor, textColor);
        stockAlarmView.setText();
        addView(stockAlarmView);
        invalidate();
    }

    private void showWebRadioView() {
        if (webRadioLayout != null) return; // already visible
        removeAllViews();
        clearViews();
        webRadioLayout = new WebRadioLayout(context, attrs);
        webRadioLayout.setCustomColor(accentColor, textColor);
        webRadioLayout.setText();
        addView(webRadioLayout);
        invalidate();
    }

    private void showAlarmView() {
        if (RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO) return;
        removeAllViews();
        clearViews();
        addView(view);
        invalidate();
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

    public void invalidate() {
        setCustomColor(accentColor, textColor);
    }
}

