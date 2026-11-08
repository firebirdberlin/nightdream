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
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
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
public class Ticker extends FrameLayout {

    private static final String TAG = Ticker.class.getSimpleName();
    private long animationSpeed = 5L;
    private final List<String> headlines = new ArrayList<>();
    private final List<String> urls = new ArrayList<>();
    private final Set<View> animatedViews = new HashSet<>();
    private final Set<Animator> animators = new HashSet<>();
    private int textColor = Color.WHITE;
    private int index = -1;
    private final Context context;
    private float textSize = 28.f;
    private boolean running = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private float downX;
    private float downY;
    private float lastX;
    private long downTime;
    private boolean isClickValid = false;
    private boolean isDragging = false;
    private String pendingUrl = null;
    private int touchSlop;
    private int longPressTimeout;

    public Ticker(Context context) {
        super(context);
        this.context = context;
        init(null);
    }

    public Ticker(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        ViewConfiguration vc = ViewConfiguration.get(context);
        touchSlop = vc.getScaledTouchSlop();
        longPressTimeout = ViewConfiguration.getLongPressTimeout();

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
        this.textColor = textColor;

        updateChildren();
    }

    public void restart() {
        Log.i(TAG, "restart()");
        for (Animator animator : new ArrayList<>(animators)) {
            animator.cancel();
        }
        animators.clear();
        animatedViews.clear();
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
        for (Animator animator : new ArrayList<>(animators)) {
            animator.cancel();
        }
        animators.clear();
        animatedViews.clear();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (oldw > 0 && w != oldw) {
            restart();
        }
    }

    public void pause() {
        Log.d(TAG, "pause()");
        for (Animator animator : new ArrayList<>(animators)) {
            animator.pause();
        }
        handler.removeCallbacksAndMessages(null);
    }

    public void resume() {
        Log.d(TAG, "resume()");
        if (Utility.areSystemAnimationsEnabled(context)) {
            for (Animator animator : new ArrayList<>(animators)) {
                animator.resume();
            }
        } else {
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(this::launchNext, 10000);
        }
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                pause();
                downX = event.getX();
                downY = event.getY();
                lastX = downX;
                downTime = event.getEventTime();
                isClickValid = true;
                isDragging = false;
                pendingUrl = resolveUrlAt(downX, downY);
                return true;

            case MotionEvent.ACTION_MOVE:
                float currentX = event.getX();
                float currentY = event.getY();
                float dx = currentX - lastX;
                float totalDx = Math.abs(currentX - downX);
                float totalDy = Math.abs(currentY - downY);

                if (totalDx > touchSlop || totalDy > touchSlop) {
                    isClickValid = false;
                    isDragging = true;
                }

                if (isDragging) {
                    for (int i = 0; i < getChildCount(); i++) {
                        View child = getChildAt(i);
                        child.setTranslationX(child.getTranslationX() + dx);
                    }
                    checkAndFillGapDuringDrag();
                }
                lastX = currentX;
                return true;

            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    resumeFromDrag();
                } else {
                    resume();
                }

                long duration = event.getEventTime() - downTime;
                if (isClickValid && !isDragging && duration <= longPressTimeout) {
                    performUrlClick(pendingUrl);
                }

                isClickValid = false;
                isDragging = false;
                pendingUrl = null;
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    resumeFromDrag();
                } else {
                    resume();
                }
                isClickValid = false;
                isDragging = false;
                pendingUrl = null;
                return true;

            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private void checkAndFillGapDuringDrag() {
        if (headlines.isEmpty()) {
            return;
        }
        float maxRight = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof TextView tv) {
                int textWidth = (int) tv.getPaint().measureText(tv.getText().toString());
                float rightEdge = tv.getTranslationX() + textWidth;
                if (rightEdge > maxRight) {
                    maxRight = rightEdge;
                }
            }
        }

        int width = getWidth();
        if (width <= 0) {
            return;
        }

        while (maxRight < width) {
            float startX = (maxRight == Float.NEGATIVE_INFINITY) ? width : maxRight;
            launchNextDuringDrag(startX);

            View newChild = getChildAt(getChildCount() - 1);
            if (newChild instanceof TextView tv) {
                int textWidth = (int) tv.getPaint().measureText(tv.getText().toString());
                maxRight = tv.getTranslationX() + textWidth;
            } else {
                break;
            }
        }
    }

    private void launchNextDuringDrag(float startX) {
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
        tv.setTranslationX(startX);
        animatedViews.add(tv);
        addView(tv);
    }

    private String resolveUrlAt(float x, float y) {
        View child = findChildViewUnder(x, y);
        if (child != null && child.getTag() instanceof Integer) {
            int itemIndex = (Integer) child.getTag();
            if (itemIndex >= 0 && itemIndex < urls.size()) {
                return urls.get(itemIndex);
            }
        }
        if (index >= 0 && index < urls.size()) {
            return urls.get(index);
        }
        return null;
    }

    private View findChildViewUnder(float x, float y) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            float left = child.getLeft() + child.getTranslationX();
            float right = left + child.getWidth();
            float top = child.getTop() + child.getTranslationY();
            float bottom = top + child.getHeight();
            if (x >= left && x <= right && y >= top && y <= bottom) {
                return child;
            }
        }
        return null;
    }

    private void performUrlClick(String url) {
        if (!Utility.isEmpty(url) && context != null) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.setData(Uri.parse(url));
            context.startActivity(browserIntent);
        }
    }

    private void resumeFromDrag() {
        Log.d(TAG, "resumeFromDrag()");
        if (!Utility.areSystemAnimationsEnabled(context)) {
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(this::launchNext, 10000);
            return;
        }

        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            v.animate().setListener(null);
            v.animate().cancel();
        }
        animators.clear();

        List<TextView> children = new ArrayList<>();
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v instanceof TextView) {
                children.add((TextView) v);
            }
        }

        if (children.isEmpty()) {
            launchNext();
            return;
        }

        boolean hasNextBeenLaunched = children.size() > 1;

        for (TextView tv : children) {
            float curX = tv.getTranslationX();
            int textWidth = (int) tv.getPaint().measureText(tv.getText().toString());
            float stage1Boundary = getWidth() - textWidth;
            float stage2Target = -textWidth;

            if (curX <= stage2Target) {
                removeView(tv);
                animatedViews.remove(tv);
            } else if (curX > stage1Boundary) {
                float dist1 = curX - stage1Boundary;
                long dur1 = Math.max(1L, (long) (dist1 * animationSpeed));
                tv.animate()
                        .translationX(stage1Boundary)
                        .setDuration(dur1)
                        .setInterpolator(new LinearInterpolator())
                        .setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(@NonNull Animator animation) {
                                animators.add(animation);
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
                            public void onAnimationRepeat(@NonNull Animator animation) {}
                        }).start();
            } else {
                float dist2 = curX - stage2Target;
                long dur2 = Math.max(1L, (long) (dist2 * animationSpeed));
                tv.animate()
                        .translationX(stage2Target)
                        .setDuration(dur2)
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
                                animators.remove(animation);
                                animatedViews.remove(tv);
                            }

                            @Override
                            public void onAnimationRepeat(@NonNull Animator animation) {}
                        }).start();
            }
        }

        if (getChildCount() == 0) {
            launchNext();
        } else if (!hasNextBeenLaunched && children.size() == 1) {
            TextView tv = children.get(0);
            int textWidth = (int) tv.getPaint().measureText(tv.getText().toString());
            if (tv.getTranslationX() <= getWidth() - textWidth) {
                launchNext();
            }
        }
    }

    @Override
    @SuppressLint("DrawAllocation")
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Paint textPaint = new Paint();
        textPaint.setTypeface(Typeface.DEFAULT);

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof TextView tv) {
                textPaint.setTextSize(tv.getTextSize());
                String text = tv.getText().toString();
                int w = (int) textPaint.measureText(text, 0, text.length());
                int h = tv.getMeasuredHeight();
                tv.layout(0, 0, w, h);
            }
        }
    }

    private void launchNext() {
        Log.i(TAG, "launchNext()");
        boolean animationsEnabled = Utility.areSystemAnimationsEnabled(context);
        int[] outIndex = {index};
        String headline = getNextHeadline(headlines, index, animationsEnabled, outIndex);
        index = outIndex[0];

        TextView tv = getTextView(animationsEnabled, headline);
        if (Utility.areSystemAnimationsEnabled(context)) {
            removeNonAnimatedViews();
            tv.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                // View's dimensions are available only after the layout pass.
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

    public static String getNextHeadline(List<String> headlines, int currentIndex, boolean animationsEnabled, int[] outIndex) {
        if (Utility.isEmpty(headlines)) {
            if (outIndex != null && outIndex.length > 0) {
                outIndex[0] = currentIndex;
            }
            return "";
        }
        int count = 0;
        int index = currentIndex;
        String headline = "";
        while (headline.isEmpty() && count < headlines.size()) {
            index = (index + 1) % headlines.size();
            headline = headlines.get(index).trim();
            count++;
        }
        if (!headline.isEmpty() && animationsEnabled) {
            headline = " +++ " + headline;
        }
        if (outIndex != null && outIndex.length > 0) {
            outIndex[0] = index;
        }
        return headline;
    }

    public static boolean isDragExceeded(float downX, float downY, float currentX, float currentY, int touchSlop) {
        float totalDx = Math.abs(currentX - downX);
        float totalDy = Math.abs(currentY - downY);
        return totalDx > touchSlop || totalDy > touchSlop;
    }

    public static boolean isValidTap(boolean isClickValid, long duration, int longPressTimeout) {
        return isClickValid && duration <= longPressTimeout;
    }

    public static boolean shouldFillGap(float maxRight, int containerWidth) {
        return maxRight < containerWidth;
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
