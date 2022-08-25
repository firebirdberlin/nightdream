package com.firebirdberlin.nightdream.ui;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.firebirdberlin.nightdream.viewmodels.RSSViewModel;
import com.firebirdberlin.nightdream.Utility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//shows text scrolling from right to left
public class Ticker extends FrameLayout {

    private static final long animationSpeed = 10L;
    public static String TAG = "Ticker";
    private final List<String> headlines = new ArrayList<>();
    private final Set<View> animatedViews = new HashSet<>();
    private final Set<Animator> animators = new HashSet<>();
    private int textSize = 72;
    private int spaceWidth = 36;
    private int textColor = Color.WHITE;
    private int accentColor = Color.WHITE;
    private int index = -1;
    private Context context;
    private Animator animator = null;
    private RSSViewModel rssViewModel = null;

    //Constructor
    public Ticker(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#AA212121"));
        background.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
        setBackground(background);

        init();
    }

    private void init() {
        rssViewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(RSSViewModel.class);
        rssViewModel.loadDataFromWorker(context, (LifecycleOwner) context);

        rssViewModel.getData().observe((LifecycleOwner) context, channel -> {
            if (channel != null) {
                if (channel.getTitle() != null) {
                    Log.d(TAG, "rss title: " + channel.getTitle());
                }
                List<String> headlines = new ArrayList<>();
                for (int i = 0; i < Math.min(channel.getArticles().size(), 5); i++) {
                    String title = channel.getArticles().get(i).getTitle();
                    String link = channel.getArticles().get(i).getLink();
                    String time = channel.getArticles().get(i).getPubDate();
                    Log.d(TAG, "rss Date: " + time);
                    Log.d(TAG, "rss Title: " + title);
                    Log.d(TAG, "rss Link: " + link);
                    headlines.add(title);
                }
                setHeadlines(headlines);
            }
        });

        addHeadline("");
        run();
    }

    //add set of headlines
    private void setHeadlines(List<String> headlines) {
        if (headlines != null && !headlines.isEmpty()) {
            this.headlines.addAll(headlines);
        }
    }

    //add one headline
    public void addHeadline(String headline) {
        this.headlines.add(headline);
    }

    //remove headline
    public void removeHeadline(int index) {
        if (headlines.size() > index) {
            this.headlines.remove(index);
        }
    }

    //return headlines size
    public int sizeHeadlines() {
        return headlines.size();
    }

    // set textColor
    public void setTextColor(int color) {
        this.textColor = color;
    }

    //set textColor, accentColor and default Background
    public void setCustomColor(int accentColor, int textColor) {
        this.accentColor = accentColor;
        this.textColor = textColor;

        updateChildren();
    }

    private void updateChildren() {
        //set all textcolor to all textviews
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View v = getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setTextColor(textColor);
                ((TextView) v).setTextSize(TypedValue.COMPLEX_UNIT_PX, this.textSize);
            }
        }
    }

    public void setSpaceWidth(int spaceWidth) {
        this.spaceWidth = spaceWidth;
    }

    //set Textsize
    public void setTextSize(int textSize) {
        this.textSize = textSize;
        updateChildren();
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
        rssViewModel.stopWorker();
    }
    public void resume() {
        Log.d(TAG, "resume()");
        for (Animator animator: animators) {
            animator.resume();
        }
        rssViewModel.loadDataFromWorker(context, (LifecycleOwner) context);
    }

    @Override
    @SuppressLint("DrawAllocation")
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Paint textPaint = new Paint();
        textPaint.setTypeface(Typeface.DEFAULT);
        textPaint.setTextSize(this.textSize);

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                String text = tv.getText().toString();
                int w = (int) textPaint.measureText(text, 0, text.length());
                int h = tv.getMeasuredHeight();
                tv.layout(0, 0, w, h);
            }
        }
    }

    private void launchNext() {
        String headline = "";
        int count = 0;
        while (headline.isEmpty() && count < headlines.size()) {
            index = (index + 1) % headlines.size();
            headline = headlines.get(index).trim();
            count++;
        }

        //if (headline.isEmpty()) return;

        Log.d(TAG, "headline: '" + headline + "'");
        TextView tv = new TextView(getContext());
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setTextColor(this.textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.textSize);
        if (!headline.isEmpty()) {
            tv.setText(String.format("+++ %s", headline));
        }
        int padding = this.textSize / 2;
        tv.setPadding(0, padding, 0, padding);
        tv.setSingleLine(true);
        tv.setTag(index);
        tv.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    // View's dimensions are available only after the layout pass.
                    // Now is the time to start animating it.
                    animationStart((TextView) v);
                }
        );
        tv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // Index identifies the clicked headline in the list.
                Log.d(TAG, "Ticker click: " + index);
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

        int distance = tv.getWidth() + spaceWidth;
        tv.animate()
                .translationXBy(-distance)
                .setDuration(distance * animationSpeed)
                .setInterpolator(new LinearInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        Log.d(TAG, "start animation");
                        //add view to set
                        animator = animation;
                        animators.add(animation);
                        animatedViews.add(tv);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        Log.d(TAG, "end animation");
                        animator = null;
                        launchNext();
                        animators.remove(animation);
                        animationEnd(tv);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        animatedViews.remove(tv);
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {}
                }).start();
    }

    private void animationEnd(final TextView tv) {
        int distance = getWidth() - spaceWidth;
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
                        removeView(tv);
                        animators.remove(animation);
                        animatedViews.remove(tv);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        animatedViews.remove(tv);
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                })
                .start();
    }
}
