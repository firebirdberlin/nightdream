package com.firebirdberlin.nightdream.ui;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.RssFeedItem;
import com.firebirdberlin.nightdream.viewmodels.RSSViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//shows text scrolling from right to left
public class Ticker extends FrameLayout implements View.OnClickListener {

    public static String TAG = "Ticker";
    private long animationSpeed = 5L;
    private final List<String> headlines = new ArrayList<>();
    private final List<String> urls = new ArrayList<>();
    private final Set<View> animatedViews = new HashSet<>();
    private final Set<Animator> animators = new HashSet<>();
    private int textColor = Color.WHITE;
    private int accentColor = Color.WHITE;
    private int index = -1;
    private final Context context;
    private float textSize = 28.f;
    private boolean running = false;
    final private Handler handler = new Handler();

    public Ticker(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#AA212121"));
        background.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
        setBackground(background);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        );
        setLayoutParams(params);

        RSSViewModel.observe(context, items -> {
            if (items != null) {
                setHeadlines(items);
            }
        });

        RSSViewModel.observeSpeed(context, speed -> {
            Log.d(TAG,"change rss speed: "+speed);
            setTickerSpeed(speed);
        });

        RSSViewModel.observeInterval(context, interval -> {
            Log.d(TAG,"change rss interval: "+interval);
            RSSViewModel.loadDataPeriodicFromWorker(context, (AppCompatActivity) context);
        });

        RSSViewModel.observeTextSize(context, textSize -> {
            Log.i(TAG,"change rss textSize: "+ textSize);
            this.textSize = textSize;
            restart();
        });
    }

    public void setHeadlines(List<RssFeedItem> items) {
        Log.i(TAG, "setHeadlines()");
        if (items != null && !items.isEmpty()) {
            this.headlines.clear();
            this.urls.clear();
            Log.d(TAG, "setHeadlines(List<RssFeedItem> articles)");
            int count = 0;
            for (RssFeedItem item : items) {
                String title = item.getTitle();
                String link = item.getLink();
                String time = item.getPubDate();
                Log.d(TAG, "rss Date: " + time);
                Log.d(TAG, "rss Title: " + title);
                Log.d(TAG, "rss Link: " + link);
                headlines.add(title);
                urls.add(link);
                if (++count >= 15) { // only up to 15 messages
                    break;
                }
            }
            restart();
        }
    }

    public void addHeadline(String headline) {
        this.headlines.add(headline);
    }

    public void setTextColor(int color) {
        this.textColor = color;
    }

    public void setBackColor(int color) {
        setBackgroundColor(color);
    }

    public void setCustomColor(int accentColor, int textColor) {
        this.accentColor = accentColor;
        this.textColor = textColor;

        updateChildren();
    }

    public void restart() {
        Log.i(TAG, "restart()");
        removeAllViewsInLayout();
        requestLayout();
        invalidate();
        run(true);
    }

    private void updateChildren() {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View v = getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setTextColor(textColor);
            }
        }
    }

    public void setTickerSpeed(Long speed) {
        Log.d(TAG, "setTickerSpeed(): " + speed);
        if (animationSpeed != speed) {
            animationSpeed = speed;
        }
    }

    //start scrolling headlines
    public void run(boolean restart) {
        if (headlines.isEmpty()) {
            Log.d(TAG, "No headlines set");
            return;
        }
        if (!this.running || restart) {
            this.running = true;
            launchNext();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacksAndMessages(null);
    }

    public void pause() {
        Log.d(TAG, "pause()");
        for (Animator animator: animators) {
            animator.pause();
        }
    }
    public void resume() {
        Log.d(TAG, "resume()");
        if(Utility.areSystemAnimationsEnabled(context) ) {
            for (Animator animator: animators) {
                animator.resume();
            }
        } else {
            launchNext();
        }
    }

    @Override
    @SuppressLint("DrawAllocation")
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Paint textPaint = new Paint();
        textPaint.setTypeface(Typeface.DEFAULT);

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                textPaint.setTextSize(tv.getTextSize());
                String text = tv.getText().toString();
                int w = (int) textPaint.measureText(text, 0, text.length());
                int h = tv.getMeasuredHeight();
                tv.layout(0, 0, w, h);
            }
        }
    }

    @Override
    public void onClick(View v) {
        Object tag = v.getTag();
        if (tag instanceof Integer && context != null) {
            String url = urls.get(index);
            if ((url != null) && (!url.isEmpty())) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                browserIntent.setData(Uri.parse(url));
                context.startActivity(browserIntent);
            }
        }
    }

    private void launchNext() {
        Log.i(TAG, "launchNext()");
        boolean animationsEnabled = Utility.areSystemAnimationsEnabled(context);
        String headline = "";
        int count = 0;
        while (headline.isEmpty() && count < headlines.size()) {
            index = (index + 1) % headlines.size();
            headline = headlines.get(index).trim();
            if (animationsEnabled) {
                headline = " +++ " + headline;
            }
            count++;
        }

        TextView tv = getTextView(animationsEnabled, headline);
        if( Utility.areSystemAnimationsEnabled(context) ) {
            removeNonAnimatedViews();
            tv.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                // View\'s dimensions are available only after the layout pass.
                // Now is the time to start animating it.
                Log.i(TAG, "addOnLayoutChangeListener");
                animationStart((TextView) v);
            });
        } else {
            removeAllViews();
            requestLayout();
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(this::launchNext, 10000);
        }
        addView(tv);
    }

    private TextView getTextView(boolean animationsEnabled, String headline) {
        TextView tv = new TextView(getContext());
        if (animationsEnabled) {
            tv.setGravity(Gravity.CENTER_VERTICAL);
            tv.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            tv.setGravity(Gravity.CENTER);
            tv.setSelected(true);
            tv.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            tv.setMarqueeRepeatLimit(101);
            tv.setHorizontallyScrolling(true);
        }
        tv.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        tv.setOnClickListener(this);
        tv.setSingleLine(true);
        tv.setTag(index);
        tv.setTextColor(this.textColor);
        tv.setTextSize(this.textSize);
        if (!headline.isEmpty()) {
            tv.setText(headline);
        }
        return tv;
    }

    void removeNonAnimatedViews() {
        for (int i=0; i < getChildCount(); i++) {
           View view = getChildAt(i) ;
            if (! animatedViews.contains(view)) {
                removeView(view);
            }
        }

    }
    private void animationStart(final TextView tv) {
        if (animatedViews.contains(tv)) {
            return;
        }

        //move view out off screen.
        tv.setTranslationX(getWidth());
        int distance = (int) tv.getPaint().measureText(tv.getText().toString());
        tv.animate()
                .translationXBy(-distance)
                .setDuration(distance * animationSpeed)
                .setInterpolator(new LinearInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(@NonNull Animator animation) {
                        Log.d(TAG, "start animation");
                        animators.add(animation);
                        animatedViews.add(tv);
                    }

                    @Override
                    public void onAnimationEnd(@NonNull Animator animation) {
                        animators.remove(animation);
                        if (tv.getParent() != null) {
                            animationEnd(tv);
                            launchNext();
                        }
                    }

                    @Override
                    public void onAnimationCancel(@NonNull Animator animation) {
                        animators.remove(animation);
                        animatedViews.remove(tv);
                    }

                    @Override
                    public void onAnimationRepeat(@NonNull Animator animation) {
                    }
                }).start();
    }

    private void animationEnd(final TextView tv) {
        int distance = getWidth();
        tv.animate()
                .translationXBy(-distance)
                .setDuration(distance * animationSpeed)
                .setInterpolator(new LinearInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(@NonNull Animator animation) {
                        animators.add(animation);
                    }

                    @Override
                    public void onAnimationEnd(@NonNull Animator animation) {
                        removeView(tv);
                        animators.remove(animation);
                        animatedViews.remove(tv);
                    }

                    @Override
                    public void onAnimationCancel(@NonNull Animator animation) {
                        //Remove view from set
                        animatedViews.remove(tv);
                        animators.remove(animation);
                    }

                    @Override
                    public void onAnimationRepeat(@NonNull Animator animation) {
                    }
                })
                .start();
    }
}
