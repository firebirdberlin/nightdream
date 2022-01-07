package com.firebirdberlin.nightdream.ui;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
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

import androidx.core.widget.TextViewCompat;

import com.firebirdberlin.nightdream.Settings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//shows text scrolling from right to left
public class Ticker extends FrameLayout implements View.OnClickListener {

    public static String TAG = "Ticker";
    private long animationSpeed = 5L;
    private final List<String> headlines = new ArrayList<>();
    private final Set<View> animatedViews = new HashSet<>();
    private HeadlineClickListener clickListener;
    private int textColor = Color.WHITE;
    private int accentColor = Color.WHITE;
    private int index = -1;
    private Context context;
    private boolean paramsWrap = false;
    private float textSize;

    //notify when a headline is clicked.
    public interface HeadlineClickListener {
        void onClick(int index);
    }

    //Constructor
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        setLayoutParams(params);
    }

    public void setLayoutParamsWrap() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        setLayoutParams(params);
        paramsWrap = true;
        restart();
    }

    public void setLayoutParamsMatch() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        setLayoutParams(params);
        paramsWrap = false;
        restart();
    }

    public boolean isParamsWrap() {
        return paramsWrap;
    }

    //add set of headlines
    public void setHeadlines(List<String> headlines) {
        Log.d(TAG, "setHeadlines(List<String> headlines)");
        if (headlines != null && !headlines.isEmpty()) {
            this.headlines.clear();
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

    //set listener
    public void setListener(HeadlineClickListener listener) {
        clickListener = listener;
    }

    // set textColor
    public void setTextColor(int color) {
        this.textColor = color;
    }

    // set BackgroundColor
    public void setBackColor(int color) {
        setBackgroundColor(color);
    }

    //set textColor, accentColor and default Background
    public void setCustomColor(int accentColor, int textColor) {
        this.accentColor = accentColor;
        this.textColor = textColor;

        updateChild();
    }

    public void restart() {
        removeAllViewsInLayout();
        requestLayout();
        invalidate();
        run();
    }

    private void updateChild() {
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

    //onClick listener
    @Override
    public void onClick(View v) {
        Object tag = v.getTag();
        if (tag instanceof Integer && clickListener != null) {
            clickListener.onClick((Integer) tag);
        }
    }

    private void launchNext() {
        Log.d(TAG, "launchNext()");
        index = (index + 1) % headlines.size();

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
        tv.setText(headlines.get(index));
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

        //view off screen.
        tv.setTranslationX(getWidth());
        tv.setText(new StringBuilder().append(tv.getText()).append(" - ").toString());

        int distance = (int) tv.getPaint().measureText(tv.getText(), 0, tv.getText().length());
        tv.animate()
                .translationXBy(-distance)
                .setDuration(distance * animationSpeed)
                .setInterpolator(new LinearInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        //add view to set
                        animatedViews.add(tv);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (tv.getParent() != null) {
                            launchNext();
                            animationEnd(tv);
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        //Remove view from set
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
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        //Remove view
                        removeView(tv);
                        animatedViews.remove(tv);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        //Remove view from set
                        animatedViews.remove(tv);
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                })
                .start();
    }

}
