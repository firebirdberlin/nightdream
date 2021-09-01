package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.RadioStreamService;

public class BottomPanelLayout extends FrameLayout {

    public static String TAG ="BottomPanelLayout";

    public boolean isVisible = true;
    Panel activePanel = Panel.ALARM_CLOCK;
    private Context context;
    private AttributeSet attrs;
    private WebRadioLayout webRadioLayout = null;
    private int accentColor;
    private int textColor;
    private AlarmClock view = null;
    private UserInteractionObserver userInteractionObserver;
    private boolean locked = false;
    private boolean showAlarmsPersistently = true;

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

    public Panel getActivePanel() {
        return activePanel;
    }

    public void setActivePanel(Panel panel) {
        activePanel = panel;
        setup();
    }

    public void setUserInteractionObserver(UserInteractionObserver o) {
        userInteractionObserver = o;
    }

    public void setUseAlarmSwipeGesture(boolean enabled) {
        if (view != null && view.alarmClockView != null) {
            view.alarmClockView.setUseAlarmSwipeGesture(enabled);
        }
    }

    public void setAlarmUseSingleTap(boolean enabled) {
        if (view != null && view.alarmClockView != null) {
            view.alarmClockView.setUseSingleTap(enabled);
        }
    }
    public void setAlarmUseLongPress(boolean enabled) {
        if (view != null && view.alarmClockView != null) {
            view.alarmClockView.setUseLongPress(enabled);
        }
    }

    public void setShowAlarmsPersistently(boolean enabled) {
        showAlarmsPersistently = enabled;
    }

    public void hide() {
        if (showAlarmsPersistently) {
            showAlarmViewIfNoRadioIsPlaying();
        } else {
            isVisible = false;
            setClickable(false);
            setAlpha(this, 0.f, 2000);
        }
    }

    private void setAlpha(View v, float alpha, int millis) {
        if (v == null) return;

        float oldValue = v.getAlpha();
        if (alpha != oldValue) {
            v.animate().setDuration(millis).alpha(alpha);
        }
    }

    public void onResume() {}

    public void show() {
        isVisible = true;
        setClickable(!locked);
    }

    public void setCustomColor(int accentColor, int textColor) {
        this.accentColor = accentColor;
        this.textColor = textColor;
        if (view != null) {
            view.setCustomColor(accentColor, textColor);
        }
        if (webRadioLayout != null) {
            webRadioLayout.setCustomColor(accentColor, textColor);
        }
    }

    public void setup() {
        if (AlarmHandlerService.alarmIsRunning()) {
            showAlarmView();
        } else if (activePanel == Panel.WEB_RADIO) {
            showWebRadioView();
        } else {
            showAlarmView();
        }
        show();
        invalidate();
    }

    private void clearViews() {
        webRadioLayout = null;
    }

    private void showWebRadioView() {

        Log.i(TAG, "showWebRadioView");

        if (webRadioLayout != null) {
            webRadioLayout.updateText();
            invalidate();
            return; // already visible
        }
        removeAllViews();
        clearViews();
        webRadioLayout = new WebRadioLayout(context, attrs);
        webRadioLayout.setCustomColor(accentColor, textColor);
        webRadioLayout.setUserInteractionObserver(userInteractionObserver);
        addView(webRadioLayout);
        invalidate();
    }

    public boolean isWebRadioViewActive() {
        return webRadioLayout != null;
    }

    private void showAlarmView() {
        if (activePanel == Panel.WEB_RADIO && !AlarmHandlerService.alarmIsRunning()) return;
        removeAllViews();
        clearViews();
        addView(view);
        invalidate();
    }

    private void showAlarmViewIfNoRadioIsPlaying() {
        if (RadioStreamService.isRunning) return;
        removeAllViews();
        clearViews();
        addView(view);
        invalidate();
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        view.setLocked(locked);
        if (webRadioLayout != null) webRadioLayout.setLocked(locked);
    }

    public AlarmClock getAlarmClock() {
        return view;
    }

    @Override
    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setClickable(clickable);
        }
    }

    public void invalidate() {
        setCustomColor(accentColor, textColor);
    }

    public enum Panel {ALARM_CLOCK, WEB_RADIO}
}

