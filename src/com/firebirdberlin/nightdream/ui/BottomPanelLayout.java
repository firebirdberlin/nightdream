package com.firebirdberlin.nightdream.ui;

import android.animation.Animator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.nightdream.services.SqliteIntentService;
import com.prof.rssparser.Article;

import java.util.ArrayList;
import java.util.List;

public class BottomPanelLayout extends FrameLayout {

    public static String TAG = "BottomPanelLayout";
    private final Context context;
    public boolean isVisible = true;
    Panel activePanel = Panel.ALARM_CLOCK;
    private AttributeSet attrs;
    private WebRadioLayout webRadioLayout = null;
    private Ticker tickerLayout = null;
    private int accentColor;
    private int textColor;
    private AlarmClock view = null;
    private UserInteractionObserver userInteractionObserver;
    private boolean locked = false;
    private boolean showAlarmsPersistently = true;
    private int paddingHorizontal = 0;
    List<String> headlinesURL = new ArrayList<>();
    private boolean useAlarmSwipeGesture = false;
    private NightDreamBroadcastReceiver broadcastReceiver = null;
    private SimpleTime nextAlarmTime;

    public BottomPanelLayout(Context context) {
        super(context);
        this.context = context;
        setClipChildren(false);
    }

    public BottomPanelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.attrs = attrs;
        setClipChildren(false);
        view = new AlarmClock(context, attrs);
        LayoutParams LLParams = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
        setLayoutParams(LLParams);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        broadcastReceiver = registerBroadcastReceiver();
        SqliteIntentService.broadcastAlarm(context);
    }
    private NightDreamBroadcastReceiver registerBroadcastReceiver() {
        NightDreamBroadcastReceiver receiver = new NightDreamBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.ACTION_ALARM_SET);
        filter.addAction(Config.ACTION_ALARM_STOPPED);
        filter.addAction(Config.ACTION_ALARM_DELETED);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);
        return receiver;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unregister(broadcastReceiver);
    }

    private void unregister(BroadcastReceiver receiver) {
        try {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
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
            useAlarmSwipeGesture = enabled;
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

    public void hideTicker() {
        Log.d(TAG, "hideTicker()");
        if (tickerLayout != null) {
            removeView(tickerLayout);
        }
    }

    public void hide() {
        Log.d(TAG, "hide()");
        if (showAlarmsPersistently) {
            showAlarmViewIfNoRadioIsPlaying();
        } else if (activePanel != Panel.TICKER) {
            isVisible = false;
            setClickable(false);
            setAlpha(this, 0.f, 2000);
        }
    }

    private void setAlpha(View v, float alpha, int millis) {
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
                            if (tickerLayout != null) {
                                showTickerView();
                                v.setAlpha(1.f);
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

    public void onResume() {
    }

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
        if (tickerLayout != null) {
            tickerLayout.setCustomColor(accentColor, textColor);
        }
    }

    public void setup() {
        Log.d(TAG, "setup()");
        if (AlarmHandlerService.alarmIsRunning()) {
            showAlarmView();
        } else if (activePanel == Panel.WEB_RADIO) {
            showWebRadioView();
        } else if (activePanel == Panel.TICKER) {
            showTickerView();
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
            removeAllViews();
            addView(webRadioLayout);
            webRadioLayout.updateText();
            invalidate();
            return; // already visible
        }

        Log.i(TAG, "showWebRadioView after");

        removeAllViews();
        clearViews();
        webRadioLayout = new WebRadioLayout(context, attrs);
        setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        webRadioLayout.setCustomColor(accentColor, textColor);
        webRadioLayout.setUserInteractionObserver(userInteractionObserver);
        addView(webRadioLayout);
        invalidate();
    }

    public void setTickerSpeed(Long speed) {
        if (tickerLayout != null) {
            tickerLayout.setTickerSpeed(speed);
        }
    }

    public void setTickerTextSize(float textSize) {
        if (tickerLayout != null) {
            tickerLayout.setTickerTextSize(textSize);
        }
    }

    public void setTickerArticles(List<Article> articles) {
        if (tickerLayout == null) {
            showTickerView();
        }
        this.headlinesURL.clear();
        List<String> headlines = new ArrayList<String>();
        for (int i = 0; i < Math.min(articles.size(), 5); i++) {
            String title = articles.get(i).getTitle();
            String link = articles.get(i).getLink();
            String time = articles.get(i).getPubDate();
            Log.d(TAG, "rss Date: " + time);
            Log.d(TAG, "rss Title: " + title);
            Log.d(TAG, "rss Link: " + link);
            headlines.add(title);
            headlinesURL.add(link);
        }
        tickerLayout.setHeadlines(headlines);
    }

    public void restartTicker(){
        if (tickerLayout != null){
            tickerLayout.restart();
        }
    }

    private void showTickerView() {
        Log.d(TAG, "showTickerView");

        if (tickerLayout != null) {
            removeAllViews();
            if (((nextAlarmTime != null) && (!nextAlarmTime.toString().isEmpty())
                    || useAlarmSwipeGesture)) {
                if (!tickerLayout.isParamsWrap()) {
                    tickerLayout.setLayoutParamsWrap();
                }
                addView(tickerLayout);
                addView(view);
            } else {
                if (tickerLayout.isParamsWrap()) {
                    tickerLayout.setLayoutParamsMatch();
                }
                addView(tickerLayout);
            }
            invalidate();
            return; // already visible
        }
        removeAllViews();
        clearViews();

        tickerLayout = new Ticker(context, attrs);
        setPadding(paddingHorizontal, 0, paddingHorizontal, 0);

        if (((nextAlarmTime != null) && (!nextAlarmTime.toString().isEmpty())
                || useAlarmSwipeGesture)) {
            if (!tickerLayout.isParamsWrap()) {
                tickerLayout.setLayoutParamsWrap();
            }
            addView(tickerLayout);
            addView(view);
        } else {
            if (tickerLayout.isParamsWrap()) {
                tickerLayout.setLayoutParamsMatch();
            }
            addView(tickerLayout);
        }

        invalidate();

        tickerLayout.addHeadline("");

        tickerLayout.setListener(new Ticker.HeadlineClickListener() {
            @Override
            public void onClick(int index) {
                // Index identifies the clicked headline in the list.
                Log.d(TAG, "Ticker click: " + index);
                if ((headlinesURL.get(index) != null)
                        && (!headlinesURL.get(index).isEmpty())) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                    browserIntent.setData(Uri.parse(headlinesURL.get(index)));
                    context.startActivity(browserIntent);
                }
            }
        });
        tickerLayout.run();
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

    private void showAlarmViewIfNoRadioIsPlaying() {
        Log.d(TAG, "showAlarmViewIfNoRadioIsPlaying");
        if (RadioStreamService.isRunning) return;
        if (activePanel == Panel.TICKER) return;

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

    class NightDreamBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, action + " received.");
            if (Config.ACTION_ALARM_SET.equals(action) ||
                    Config.ACTION_ALARM_STOPPED.equals(action) ||
                    Config.ACTION_ALARM_DELETED.equals(action)) {
                Bundle extras = intent.getExtras();
                nextAlarmTime = null;
                if (extras != null) {
                    nextAlarmTime = new SimpleTime(extras);
                }
            }
        }
    }

    public void invalidate() {
        setCustomColor(accentColor, textColor);
    }

    public enum Panel {ALARM_CLOCK, WEB_RADIO, TICKER}
}

