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
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.TextViewCompat;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.viewmodels.RSSViewModel;
import com.prof.rssparser.Article;

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
    private Context context;
    private float textSize;

    public Ticker(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#AA212121"));
        background.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
        setBackground(background);

        Settings settings = new Settings(context);
        animationSpeed = settings.rssTickerSpeed;
        textSize = settings.rssTextSize;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        );
        setLayoutParams(params);

        RSSViewModel.observe(context, channel -> {
            if (channel != null) {
                setHeadlines(channel.getArticles());
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
            Log.d(TAG,"change rss textSize: "+textSize);
            setTickerTextSize(textSize);
        });
        //headlines.add("");
    }

    public void setHeadlines(List<Article> articles) {
        if (articles != null && !articles.isEmpty()) {
            this.headlines.clear();
            this.urls.clear();
            this.headlines.addAll(headlines);
            this.urls.addAll(urls);
            Log.d(TAG, "setHeadlines(List<String> headlines)");
            for (int i = 0; i < Math.min(articles.size(), 10); i++) {
                String title = articles.get(i).getTitle();
                String link = articles.get(i).getLink();
                String time = articles.get(i).getPubDate();
                Log.d(TAG, "rss Date: " + time);
                Log.d(TAG, "rss Title: " + title);
                Log.d(TAG, "rss Link: " + link);
                headlines.add(title);
                urls.add(link);
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
        removeAllViewsInLayout();
        requestLayout();
        invalidate();
        run();
    }

    private void updateChildren() {
        //set all textcolor to all textviews
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

    public void setTickerTextSize(float textSize) {
        Log.d(TAG, "setTickerTextSize(): " + textSize);
        if (this.textSize != textSize) {
            this.textSize = textSize;
            restart();
        }
    }

    //start scrolling headlines
    public void run() {
        if (headlines.isEmpty()) {
            Log.d(TAG, "No headlines set");
            return;
        }
        launchNext();
    }

    public void pause() {
        Log.d(TAG, "pause()");
        for (Animator animator: animators) {
            animator.pause();
        }
    }
    public void resume() {
        Log.d(TAG, "resume()");
        for (Animator animator: animators) {
            animator.resume();
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
            if ((url != null) && (!urls.isEmpty())) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                browserIntent.setData(Uri.parse(url));
                context.startActivity(browserIntent);
            }
        }
    }

    private void launchNext() {
        String headline = "";
        int count = 0;
        while (headline.isEmpty() && count < headlines.size()) {
            index = (index + 1) % headlines.size();
            headline = " +++ " + headlines.get(index).trim();
            count++;
        }

        //make TextView
        TextView tv = new TextView(getContext());
        tv.setGravity(Gravity.CENTER_VERTICAL);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(tv, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        if (textSize > 0) {
            float textSize = tv.getTextSize();
            if (textSize < 30) textSize= textSize * 1.5f; //must be for different display resolutions
            TextViewCompat.setAutoSizeTextTypeWithDefaults(tv, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
            tv.setTextSize(textSize / this.textSize);
        }
        tv.setTextColor(this.textColor);
        if (!headline.isEmpty()) {
            tv.setText(headline);
        }
        tv.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        tv.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        tv.setSingleLine(true);
        tv.setTag(index);
        tv.setOnClickListener(this);
        tv.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top,
                                       int right, int bottom,
                                       int oldLeft, int oldTop,
                                       int oldRight, int oldBottom) {
                // View's dimensions are available only after the layout pass.
                // Now is the time to start animating it.
                animationStart((TextView) v);
            }
        });
        addView(tv);
    }

    private void animationStart(final TextView tv) {

        if (animatedViews.contains(tv)) {
            return;
        }

        //move view out off screen.
        tv.setTranslationX(getWidth());

        int distance = (int) tv.getPaint().measureText(tv.getText(), 0, tv.getText().length());
        tv.animate()
                .translationXBy(-distance)
                .setDuration(distance * animationSpeed)
                .setInterpolator(new LinearInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        Log.d(TAG, "start animation");
                        animators.add(animation);
                        animatedViews.add(tv);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animators.remove(animation);
                        if (tv.getParent() != null) {
                            launchNext();
                            animationEnd(tv);
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        animators.remove(animation);
                        animatedViews.remove(tv);
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                })
                .start();
    }

    private void animationEnd(final TextView tv) {
        int distance = getWidth();
        tv.animate()
                .translationXBy(-distance)
                .setDuration(distance * animationSpeed)
                .setInterpolator(new LinearInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        animators.add(animation);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        //Remove view
                        removeView(tv);
                        animators.remove(animation);
                        animatedViews.remove(tv);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        //Remove view from set
                        animatedViews.remove(tv);
                        animators.remove(animation);
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                })
                .start();
    }
}
