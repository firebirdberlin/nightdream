/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.viewmodels.AlarmClockViewModel;


public class BottomPanelLayout extends FrameLayout {
    private static final String TAG = BottomPanelLayout.class.getSimpleName();

    private AlarmClock view = null;
    private Panel activePanel = Panel.ALARM_CLOCK;
    private Ticker tickerLayout = null;
    private UserInteractionObserver userInteractionObserver;
    private WebRadioLayout webRadioLayout = null;
    private boolean locked = false;
    private boolean rssEnabled = false;
    private boolean notifyForUpcomingAlarms = false;
    private boolean showAlarmsPersistently = true;
    private boolean tickerDelayPassed = false;
    private final Context context;
    private View self;
    private int accentColor;
    private int paddingHorizontal = 0;
    private int textColor;
    public boolean isVisible = true;
    private SimpleTime nextAlarmTime = null;

    final private Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hidePanel = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "hidePanel");
            handler.removeCallbacks(this);
            if (showAlarmsPersistently) {
                setup();
            } else {
                isVisible = false;
                setClickable(false);
                setAlpha(self, 0.f, 2000);
                if (tickerLayout != null) {
                    tickerLayout.pause();
                }
            }
        }
    };

    private final Runnable reevaluatePanelRunnable = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(this);
            Log.d(TAG, "reevaluatePanelRunnable");
            setup();
        }
    };

    private final Runnable showTickerAfterDelay = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(this);
            Log.d(TAG, "showTickerAfterDelay");
            tickerDelayPassed = true;
            setup();
        }
    };

    public BottomPanelLayout(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public BottomPanelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
        view = new AlarmClock(context);
    }

    private void init() {
        this.self = this;
        setClipChildren(false);
        AlarmClockViewModel.observeNextAlarm(context, nextAlarm -> {
            nextAlarmTime = nextAlarm;
            if (nextAlarm != null) {
                Log.d(TAG, "next alarm: " + nextAlarm);

                if (nextAlarmTime.getRemainingMillis() > 60 * 60000) {
                    // re-evaluate panel one hour before the next alarm
                    long timeToStop = nextAlarmTime.getRemainingMillis() - 60 * 60000L;
                    Log.d(TAG, "reevaluatePanel in " + timeToStop);
                    handler.removeCallbacks(reevaluatePanelRunnable);
                    handler.postDelayed(reevaluatePanelRunnable, timeToStop);
                }
            } else {
                Log.d(TAG, "no next alarm");
            }
            setup();
        });
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(hidePanel);
        handler.removeCallbacks(reevaluatePanelRunnable);
        handler.removeCallbacks(showTickerAfterDelay);
    }

    public void setPaddingHorizontal(int paddingHorizontal) {
        this.paddingHorizontal = paddingHorizontal;
        if (view != null) {
            setPadding(0, 0, 0, 0);
            view.setPaddingHorizontal(paddingHorizontal);
            view.invalidate();
        }
        if (webRadioLayout != null) {
            setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
            webRadioLayout.invalidate();
        }
        invalidate();
    }

    public Panel getActivePanel() {
        return activePanel;
    }

    public void setActivePanel(Panel panel) {
        Log.d(TAG, "setActivePanel: "+panel);
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

    public void setAlarmUseSlider(boolean enabled) {
        if (view != null) {
            view.setUseAlarmStopSlider(enabled);
        }
        setup();
    }

    public void setShowAlarmsPersistently(boolean enabled) {
        showAlarmsPersistently = enabled;
    }

    public void setRssEnabled(boolean enabled) {
        if (!enabled) {
            tickerDelayPassed = false;
        }
        rssEnabled = enabled;
    }

    public void setNotifyForUpComingAlarms(boolean enabled) {
       notifyForUpcomingAlarms = enabled;
    }

    @NonNull
    public Panel determineActivePanel() {
        if (AlarmHandlerService.alarmIsRunning()) {
            return Panel.ALARM_CLOCK;
        }
        if (RadioStreamService.isRunning || activePanel == Panel.WEB_RADIO) {
            return Panel.WEB_RADIO;
        }
        if (notifyForUpcomingAlarms && nextAlarmTime != null && nextAlarmTime.getRemainingMillis() <= 60 * 60000) {
            return Panel.ALARM_CLOCK;
        }
        if (rssEnabled && (tickerLayout != null || tickerDelayPassed)) {
            return Panel.TICKER;
        }
        return Panel.ALARM_CLOCK;
    }

    public void hide() {
        Log.d(TAG, "hide()");
        if (tickerLayout == null && (nextAlarmTime == null || !notifyForUpcomingAlarms || nextAlarmTime.getRemainingMillis() > 60 * 60000)) {
            handler.post(hidePanel);
        } else {
            // the ticker is active or upcoming alarm is near. It should remain visible.
            handler.postDelayed(hidePanel, 60 * 60000);
        }
    }

    private void setAlpha(View v, float alpha, int millis) {
        Log.d(TAG, "setAlpha()");
        if (v == null) return;

        float oldValue = v.getAlpha();
        if (alpha != oldValue) {
            v.animate().setDuration(millis).alpha(alpha).start();
        }
    }

    public void show() {
        handler.removeCallbacks(hidePanel);
        isVisible = true;
        setClickable(!locked);
        if (self != null) {
            self.setAlpha(1.0f);
        }
        if (tickerLayout != null) {
            tickerLayout.resume();
        }
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
        if (tickerLayout != null) {
            tickerLayout.setCustomColor(accentColor, textColor);
        }
    }

    public void setup() {
        Log.d(TAG, "setup()");
        handler.removeCallbacks(showTickerAfterDelay);
        Panel panel = determineActivePanel();
        if (panel == Panel.ALARM_CLOCK) {
            showAlarmView();
            if (rssEnabled && tickerLayout == null && !tickerDelayPassed && !AlarmHandlerService.alarmIsRunning() && !RadioStreamService.isRunning && !(notifyForUpcomingAlarms && nextAlarmTime != null && nextAlarmTime.getRemainingMillis() <= 60 * 60000)) {
                handler.postDelayed(showTickerAfterDelay, 30000L);
            }
        } else if (panel == Panel.WEB_RADIO) {
            showWebRadioView();
        } else if (panel == Panel.TICKER) {
            showTickerView();
        }
        show();
        invalidate();
    }

    private void clearViews() {
        webRadioLayout = null;
        tickerLayout = null;
    }

    private void showWebRadioView() {
        Log.i(TAG, "showWebRadioView");

        if (webRadioLayout != null) {
            removeAllViews();
            addView(webRadioLayout);
            webRadioLayout.updateText();
            invalidate();
            return; // already visible
        }
        removeAllViews();
        clearViews();
        webRadioLayout = new WebRadioLayout(context);
        setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        webRadioLayout.setCustomColor(accentColor, textColor);
        webRadioLayout.setUserInteractionObserver(userInteractionObserver);
        addView(webRadioLayout);
        invalidate();
    }

    private void showTickerView() {
        Log.d(TAG, "showTickerView()");

        if (tickerLayout != null && tickerLayout.getParent() == this) {
            show();
            return;
        }

        if (tickerLayout != null) {
            removeAllViews();
            addView(tickerLayout);
            show();
            return;
        }
        removeAllViews();
        clearViews();

        tickerLayout = new Ticker(context);

        setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        addView(tickerLayout);
        invalidate();
        tickerLayout.run(false);
        show();
    }

    public boolean isWebRadioViewActive() {
        return webRadioLayout != null;
    }

    private void showAlarmView() {
        Log.d(TAG, "showAlarmView()");
        if (activePanel == Panel.WEB_RADIO && !AlarmHandlerService.alarmIsRunning()) return;
        removeAllViews();
        clearViews();
        setPadding(0, 0, 0, 0);
        view.setPaddingHorizontal(paddingHorizontal);
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

    public enum Panel {ALARM_CLOCK, WEB_RADIO, TICKER}
}

