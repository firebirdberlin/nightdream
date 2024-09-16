package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.viewmodels.AlarmClockViewModel;


public class BottomPanelLayout extends FrameLayout {
    private static final String TAG = "BottomPanelLayout";

    private AlarmClock view = null;
    private AttributeSet attrs;
    private Panel activePanel = Panel.ALARM_CLOCK;
    private Ticker tickerLayout = null;
    private UserInteractionObserver userInteractionObserver;
    private WebRadioLayout webRadioLayout = null;
    private boolean locked = false;
    private boolean rssEnabled = false;
    private boolean notifyForUpcomingAlarms = false;
    private boolean showAlarmsPersistently = true;
    private final Context context;
    private View self;
    private int accentColor;
    private int paddingHorizontal = 0;
    private int textColor;
    public boolean isVisible = true;
    private SimpleTime nextAlarmTime = null;

    final private Handler handler = new Handler();
    private final Runnable hidePanel = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "hidePanel");
            handler.removeCallbacks(hidePanel);
            if (showAlarmsPersistently) {
                showAlarmViewIfNoRadioIsPlaying();
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

    private final Runnable hideTicker = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(hideTicker);
            Log.d(TAG, "hideTicker");
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
        this.attrs = attrs;
        view = new AlarmClock(context, attrs);
    }

    private void init() {
        this.self = this;
        setClipChildren(false);
        AlarmClockViewModel.observeNextAlarm(context, nextAlarm -> {
            nextAlarmTime = nextAlarm;
            if (nextAlarm != null) {
                Log.d(TAG, "next alarm: " + nextAlarm);

                if (nextAlarmTime.getRemainingMillis() > 60 * 60000) {
                    // hide the ticker one hour before the next alarm
                    long timeToStop = nextAlarmTime.getRemainingMillis() - 60 * 60000L;
                    Log.d(TAG, "hideTicker in " + timeToStop);
                    handler.removeCallbacks(hideTicker);
                    handler.postDelayed(hideTicker, timeToStop);
                }
            } else {
                Log.d(TAG, "no next alarm");
                if (tickerLayout != null && shallShowTicker()) {
                    Log.d(TAG, "init() -> showTickerView()");
                    showTickerView();
                }
            }
        });
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(hidePanel);
        handler.removeCallbacks(hideTicker);
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

    public void setShowAlarmsPersistently(boolean enabled) {
        showAlarmsPersistently = enabled;
    }

    public void setRssEnabled(boolean enabled) {
        rssEnabled = enabled;
    }

    public void setNotifyForUpComingAlarms(boolean enabled) {
       notifyForUpcomingAlarms = enabled;
    }

    public void hide() {
        Log.d(TAG, "hide()");
        if (tickerLayout == null && mayHideAlarm()) {
            handler.post(hidePanel);
        } else {
            Log.d(TAG, "hide() activePanel: "+activePanel);
            if (rssEnabled && (nextAlarmTime == null || mayHideAlarm())) {
                isVisible = false;
                setClickable(false);
                setAlpha(this, 0.f, 2000);
            }
        }
    }

    private void showAlarmViewIfNoRadioIsPlaying() {
        Log.d(TAG, "showAlarmViewIfNoRadioIsPlaying");
        if ((RadioStreamService.isRunning)
                || (activePanel == Panel.TICKER)) return;

        removeAllViews();
        clearViews();
        addView(view);
        invalidate();
    }

    private void setAlpha(View v, float alpha, int millis) {
        Log.d(TAG, "setAlpha()");
        if (v == null) return;

        float oldValue = v.getAlpha();
        if (alpha != oldValue) {
            v.animate()
                    .setDuration(millis)
                    .alpha(alpha)
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            Log.d(TAG, "setAlpha() onAnimationEnd");
                            if (rssEnabled) {
                                Log.d(TAG, "setAlpha() showTicker after animation end");
                                showTickerView();
                                v.setAlpha(1.f);
                                handler.postDelayed(hidePanel, 60 * 60000);
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }
                    }).start();
        }
    }

    public void show() {
        handler.removeCallbacks(hidePanel);
        isVisible = true;
        setClickable(!locked);
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
        if (AlarmHandlerService.alarmIsRunning()) {
            showAlarmView();
        } else if (RadioStreamService.isRunning || activePanel == Panel.WEB_RADIO) {
            showWebRadioView();
        } else {
            if (shallShowTicker() && (nextAlarmTime == null || nextAlarmTime.getRemainingMillis() < 60 * 60000)) {
                showTickerView();
            } else {
                Log.d(TAG, "setup() nextAlarmTime: "+nextAlarmTime);
                    showAlarmView();
           }
        }
        show();
        invalidate();
    }

    private boolean shallShowTicker() {
        return rssEnabled && mayHideAlarm();
    }

    private boolean mayHideAlarm() {
        Log.i(TAG, "mayHIdeAlarm() -> nextAlarmTIme: " + nextAlarmTime);
        return (
                nextAlarmTime == null
                        || !notifyForUpcomingAlarms
                        || nextAlarmTime.getRemainingMillis() > 60 * 60000
        );
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
        webRadioLayout = new WebRadioLayout(context, attrs);
        setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        webRadioLayout.setCustomColor(accentColor, textColor);
        webRadioLayout.setUserInteractionObserver(userInteractionObserver);
        addView(webRadioLayout);
        invalidate();
    }

    private void showTickerView() {
        Log.d(TAG, "showTickerView()");

        removeAllViews();
        if (tickerLayout != null) {
            addView(tickerLayout);
            return; // already visible
        }
        clearViews();

        tickerLayout = new Ticker(context, attrs);

        setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        addView(tickerLayout);
        invalidate();
        tickerLayout.run(false);
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

