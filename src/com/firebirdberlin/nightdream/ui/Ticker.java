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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//shows text scrolling from right to left
public class Ticker extends FrameLayout implements View.OnClickListener {

    public static String TAG = "Ticker";
    private static final long animationSpeed = 10L;
    private final List<String> headlines = new ArrayList<>();
    private final Set<View> animatedViews = new HashSet<>();
    private HeadlineClickListener clickListener;
    private int textSize = 72;
    private int spaceWidth = 36;
    private int textColor = Color.WHITE;
    private int accentColor = Color.WHITE;
    private int index=-1;
    private Context context;

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
    }

    //add set of headlines
    public void setHeadlines(List<String> headlines) {
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

    //set listener
    public void setListener(HeadlineClickListener listener) {
        clickListener = listener;
    }

    // set textColor
    public void setTextColor(int color) {
        this.textColor = color;
    }

    public void setBackColor(int color) {
        setBackgroundColor(color);
    }

    //set textColor, accentColor and default Background
    public void setCustomColor(int accentColor, int textColor) {
        this.accentColor = accentColor;
        this.textColor = textColor;

        updateChild();
    }

    private void updateChild(){
        //set all textcolor to all textviews
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View v = getChildAt(i);
            if (v instanceof TextView){
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
        updateChild();
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

    //onClick listener
    @Override
    public void onClick(View v) {
        Object tag = v.getTag();
        if (tag instanceof Integer && clickListener != null) {
            clickListener.onClick((Integer)tag);
        }
    }

    private void launchNext() {
        index = (index + 1) % headlines.size();

        //make TextView
        TextView tv = new TextView(getContext());
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setTextColor(this.textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.textSize);
        tv.setText(headlines.get(index));
        int padding = (int)this.textSize / 2;
        tv.setPadding(0, padding, 0, padding);
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
                animationStart((TextView)v);
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

        int distance = tv.getWidth() + spaceWidth;
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
                        launchNext();
                        animationEnd(tv);
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
        int distance = getWidth() - spaceWidth;
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
