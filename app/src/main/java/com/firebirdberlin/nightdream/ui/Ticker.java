/*
 * NightDream
 * Copyright (C) 2026 Stefan Fruhner
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
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.events.AppEventBus;
import com.firebirdberlin.nightdream.events.OnPowerConnectionChanged;
import com.firebirdberlin.nightdream.models.RssFeedItem;
import com.firebirdberlin.nightdream.viewmodels.RSSViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

//shows text scrolling from right to left
public class Ticker extends FrameLayout {

    private static final String TAG = Ticker.class.getSimpleName();
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TickerAnimationController animationController;
    private final TickerTouchHandler touchHandler;

    public Ticker(Context context) {
        super(context);
        this.context = context;
        this.animationController = new TickerAnimationController(context);
        this.touchHandler = new TickerTouchHandler(ViewConfiguration.get(context));
        init(null);
    }

    public Ticker(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.animationController = new TickerAnimationController(context);
        this.touchHandler = new TickerTouchHandler(ViewConfiguration.get(context));
        init(attrs);
    }

    private void init(AttributeSet attrs) {
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
            Log.d(TAG, "change rss speed: " + speed);
            setTickerSpeed(speed);
        });

        RSSViewModel.observeInterval(context, interval -> {
            Log.d(TAG, "change rss interval: " + interval);
            RSSViewModel.loadDataPeriodicFromWorker(context, (AppCompatActivity) context);
        });

        RSSViewModel.observeTextSize(context, textSize -> {
            Log.i(TAG, "change rss textSize: " + textSize);
            animationController.setTextSize(textSize);
            restart();
        });
    }

    public void setHeadlines(List<RssFeedItem> items) {
        Log.i(TAG, "setHeadlines()");
        if (items != null && !items.isEmpty()) {
            animationController.setHeadlines(items);
            restart();
        }
    }

    public void addHeadline(String headline) {
        animationController.addHeadline(headline);
    }

    public void setTextColor(int color) {
        animationController.setTextColor(color);
    }

    public void setBackColor(int color) {
        setBackgroundColor(color);
    }

    public void setCustomColor(int accentColor, int textColor) {
        animationController.setTextColor(textColor);
        updateChildren();
    }

    public void setTickerSpeed(Long speed) {
        Log.d(TAG, "setTickerSpeed(): " + speed);
        animationController.setTickerSpeed(speed);
    }

    public void restart() {
        Log.i(TAG, "restart()");
        animationController.setPaused(false);
        handler.removeCallbacksAndMessages(null);
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.animate().setListener(null);
            child.animate().cancel();
        }
        animationController.cancelAnimations();
        removeAllViews();
        requestLayout();
        invalidate();
        run(true);
    }

    private void updateChildren() {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View v = getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setTextColor(animationController.getTextColor());
            }
        }
    }

    //start scrolling headlines
    public void run(boolean restart) {
        if (animationController.getHeadlines().isEmpty()) {
            Log.d(TAG, "No headlines set");
            return;
        }
        if (!animationController.isRunning() || restart) {
            animationController.setRunning(true);
            launchNext();
        }
    }

    private final Consumer<Object> eventListener = event -> {
        if (event instanceof OnPowerConnectionChanged) {
            Log.d(TAG, "OnPowerConnectionChanged received: "
                    + ((OnPowerConnectionChanged) event).isConnected);
            restart();
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        AppEventBus.addListener(eventListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        AppEventBus.removeListener(eventListener);
        handler.removeCallbacksAndMessages(null);
        animationController.cancelAnimations();
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
        animationController.setPaused(true);
        animationController.pauseAnimations();
        handler.removeCallbacksAndMessages(null);
    }

    public void resume() {
        Log.d(TAG, "resume()");
        if (!animationController.isPaused()) {
            return;
        }
        animationController.setPaused(false);
        if (animationController.isAnimationActive()) {
            animationController.resumeAnimations();
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
                touchHandler.setDownX(event.getX());
                touchHandler.setDownY(event.getY());
                touchHandler.setLastX(touchHandler.getDownX());
                touchHandler.setDownTime(event.getEventTime());
                touchHandler.setClickValid(true);
                touchHandler.setDragging(false);
                touchHandler.setPendingUrl(resolveUrlAt(touchHandler.getDownX(), touchHandler.getDownY()));
                return true;

            case MotionEvent.ACTION_MOVE:
                float currentX = event.getX();
                float currentY = event.getY();
                float dx = currentX - touchHandler.getLastX();
                float totalDx = Math.abs(currentX - touchHandler.getDownX());
                float totalDy = Math.abs(currentY - touchHandler.getDownY());

                if (totalDx > touchHandler.getTouchSlop() || totalDy > touchHandler.getTouchSlop()) {
                    touchHandler.setClickValid(false);
                    if (!touchHandler.isDragging()) {
                        touchHandler.setDragging(true);
                        pause();
                    }
                }

                if (touchHandler.isDragging()) {
                    for (int i = 0; i < getChildCount(); i++) {
                        View child = getChildAt(i);
                        child.setTranslationX(child.getTranslationX() + dx);
                    }
                    if (animationController.isAnimationActive()) {
                        checkAndFillGapDuringDrag();
                    }
                }
                touchHandler.setLastX(currentX);
                return true;

            case MotionEvent.ACTION_UP:
                if (touchHandler.isDragging()) {
                    resumeFromDrag();
                }

                long duration = event.getEventTime() - touchHandler.getDownTime();
                if (TickerTouchHandler.isValidTap(touchHandler.isClickValid(), duration, touchHandler.getLongPressTimeout())) {
                    performUrlClick(touchHandler.getPendingUrl());
                }

                touchHandler.reset();
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (touchHandler.isDragging()) {
                    resumeFromDrag();
                }
                touchHandler.reset();
                return true;

            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private void checkAndFillGapDuringDrag() {
        if (Utility.isEmpty(animationController.getHeadlines())) {
            return;
        }
        int width = getWidth();
        if (width <= 0) {
            return;
        }

        boolean animationsEnabled = animationController.isAnimationActive();

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

        float minLeft = Float.POSITIVE_INFINITY;
        TextView firstChild = null;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof TextView tv) {
                float leftEdge = tv.getTranslationX();
                if (leftEdge < minLeft) {
                    minLeft = leftEdge;
                    firstChild = tv;
                }
            }
        }

        while (minLeft > 0 && firstChild != null) {
            int firstIndex = animationController.getIndex();
            if (firstChild.getTag() instanceof Integer) {
                firstIndex = (Integer) firstChild.getTag();
            }

            int prevIndex = firstIndex;
            int count = 0;
            String headline = "";
            List<String> headlines = animationController.getHeadlines();
            while (headline.isEmpty() && count < headlines.size()) {
                prevIndex = (prevIndex - 1 + headlines.size()) % headlines.size();
                headline = headlines.get(prevIndex).trim();
                count++;
            }

            if (headline.isEmpty()) {
                break;
            }

            if (animationsEnabled) {
                headline = " +++ " + headline;
            }

            TextView tv = getTextView(animationsEnabled, headline, prevIndex);
            int textWidth = (int) tv.getPaint().measureText(tv.getText().toString());
            float startX = minLeft - textWidth;
            tv.setTranslationX(startX);
            animationController.addAnimatedView(tv);
            addView(tv, 0);

            minLeft = startX;
            firstChild = tv;
        }
    }

    private void launchNextDuringDrag(float startX) {
        boolean animationsEnabled = animationController.isAnimationActive();
        int[] outIndex = {animationController.getIndex()};
        String headline = getNextHeadline(animationController.getHeadlines(), animationController.getIndex(), animationsEnabled, outIndex);
        animationController.setIndex(outIndex[0]);

        TextView tv = getTextView(animationsEnabled, headline, animationController.getIndex());
        tv.setTranslationX(startX);
        animationController.addAnimatedView(tv);
        addView(tv);
    }

    private String resolveUrlAt(float x, float y) {
        View child = findChildViewUnder(x, y);
        List<String> urls = animationController.getUrls();
        if (child != null && child.getTag() instanceof Integer) {
            int itemIndex = (Integer) child.getTag();
            if (itemIndex >= 0 && itemIndex < urls.size()) {
                return urls.get(itemIndex);
            }
        }
        int currentIndex = animationController.getIndex();
        if (currentIndex >= 0 && currentIndex < urls.size()) {
            return urls.get(currentIndex);
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
        if (!animationController.isAnimationActive()) {
            handler.removeCallbacksAndMessages(null);
            float dragDistance = touchHandler.getLastX() - touchHandler.getDownX();
            if (dragDistance < -touchHandler.getTouchSlop()) {
                animateToNextHeadline();
            } else if (dragDistance > touchHandler.getTouchSlop()) {
                animateToPreviousHeadline();
            } else {
                launchCurrent();
            }
            return;
        }

        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            v.animate().setListener(null);
            v.animate().cancel();
        }
        animationController.cancelAnimations();

        checkAndFillGapDuringDrag();

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

        children.sort((v1, v2) -> Float.compare(v1.getTranslationX(), v2.getTranslationX()));

        TextView rightmostView = children.get(children.size() - 1);

        for (TextView tv : children) {
            float curX = tv.getTranslationX();
            int textWidth = (int) tv.getPaint().measureText(tv.getText().toString());
            float stage1Boundary = getWidth() - textWidth;
            float stage2Target = -textWidth;

            boolean isRightmost = (tv == rightmostView);

            if (curX <= stage2Target) {
                removeView(tv);
                animationController.removeAnimatedView(tv);
            } else if (isRightmost && curX > stage1Boundary) {
                float dist1 = curX - stage1Boundary;
                long dur1 = Math.max(1L, (long) (dist1 * animationController.getAnimationSpeed()));
                tv.animate()
                        .translationX(stage1Boundary)
                        .setDuration(dur1)
                        .setInterpolator(new LinearInterpolator())
                        .setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(@NonNull Animator animation) {
                                animationController.addAnimator(animation);
                                animationController.addAnimatedView(tv);
                            }

                            @Override
                            public void onAnimationEnd(@NonNull Animator animation) {
                                animationController.removeAnimator(animation);
                                if (tv.getParent() != null) {
                                    animationEnd(tv);
                                    launchNext();
                                }
                            }

                            @Override
                            public void onAnimationCancel(@NonNull Animator animation) {
                                animationController.removeAnimator(animation);
                                animationController.removeAnimatedView(tv);
                            }

                            @Override
                            public void onAnimationRepeat(@NonNull Animator animation) {}
                        }).start();
            } else {
                float dist2 = curX - stage2Target;
                long dur2 = Math.max(1L, (long) (dist2 * animationController.getAnimationSpeed()));
                tv.animate()
                        .translationX(stage2Target)
                        .setDuration(dur2)
                        .setInterpolator(new LinearInterpolator())
                        .setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(@NonNull Animator animation) {
                                animationController.addAnimator(animation);
                                animationController.addAnimatedView(tv);
                            }

                            @Override
                            public void onAnimationEnd(@NonNull Animator animation) {
                                removeView(tv);
                                animationController.removeAnimator(animation);
                                animationController.removeAnimatedView(tv);
                            }

                            @Override
                            public void onAnimationCancel(@NonNull Animator animation) {
                                animationController.removeAnimator(animation);
                                animationController.removeAnimatedView(tv);
                            }

                            @Override
                            public void onAnimationRepeat(@NonNull Animator animation) {}
                        }).start();

                if (isRightmost && curX <= stage1Boundary) {
                    launchNext();
                }
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
        boolean animationsEnabled = animationController.isAnimationActive();
        int[] outIndex = {animationController.getIndex()};
        String headline = getNextHeadline(animationController.getHeadlines(), animationController.getIndex(), animationsEnabled, outIndex);
        animationController.setIndex(outIndex[0]);
        displayHeadline(animationsEnabled, headline);
    }

    private void animateToNextHeadline() {
        Log.i(TAG, "animateToNextHeadline()");
        int width = getWidth() > 0 ? getWidth() : 1000;
        int[] outIndex = {animationController.getIndex()};
        String headline = getNextHeadline(animationController.getHeadlines(), animationController.getIndex(), false, outIndex);
        animationController.setIndex(outIndex[0]);

        final View oldView = getChildCount() > 0 ? getChildAt(getChildCount() - 1) : null;
        if (oldView != null) {
            oldView.animate()
                    .translationX(-width)
                    .setDuration(250)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> removeView(oldView))
                    .start();
        }

        TextView newTv = getTextView(false, headline);
        newTv.setTranslationX(width);
        addView(newTv);
        newTv.animate()
                .translationX(0)
                .setDuration(250)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::launchNext, 10000);
    }

    private void animateToPreviousHeadline() {
        Log.i(TAG, "animateToPreviousHeadline()");
        int width = getWidth() > 0 ? getWidth() : 1000;
        int[] outIndex = {animationController.getIndex()};
        String headline = getPreviousHeadline(animationController.getHeadlines(), animationController.getIndex(), false, outIndex);
        animationController.setIndex(outIndex[0]);

        final View oldView = getChildCount() > 0 ? getChildAt(getChildCount() - 1) : null;
        if (oldView != null) {
            oldView.animate()
                    .translationX(width)
                    .setDuration(250)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> removeView(oldView))
                    .start();
        }

        TextView newTv = getTextView(false, headline);
        newTv.setTranslationX(-width);
        addView(newTv);
        newTv.animate()
                .translationX(0)
                .setDuration(250)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::launchNext, 10000);
    }

    private void launchPrevious() {
        Log.i(TAG, "launchPrevious()");
        boolean animationsEnabled = animationController.isAnimationActive();
        int[] outIndex = {animationController.getIndex()};
        String headline = getPreviousHeadline(animationController.getHeadlines(), animationController.getIndex(), animationsEnabled, outIndex);
        animationController.setIndex(outIndex[0]);
        displayHeadline(animationsEnabled, headline);
    }

    private void launchCurrent() {
        Log.i(TAG, "launchCurrent()");
        boolean animationsEnabled = animationController.isAnimationActive();
        int currentIndex = animationController.getIndex();
        List<String> headlines = animationController.getHeadlines();
        if (currentIndex < 0 || currentIndex >= headlines.size()) {
            launchNext();
            return;
        }
        if (!animationsEnabled && getChildCount() > 0) {
            View oldView = getChildAt(getChildCount() - 1);
            oldView.animate()
                    .translationX(0)
                    .setDuration(150)
                    .start();
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(this::launchNext, 10000);
            return;
        }
        String headline = headlines.get(currentIndex).trim();
        if (!headline.isEmpty() && animationsEnabled) {
            headline = " +++ " + headline;
        }
        displayHeadline(animationsEnabled, headline);
    }

    private void displayHeadline(boolean animationsEnabled, String headline) {
        TextView tv = getTextView(animationsEnabled, headline);
        if (animationsEnabled) {
            removeNonAnimatedViews();
            tv.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
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

    public static String getPreviousHeadline(List<String> headlines, int currentIndex, boolean animationsEnabled, int[] outIndex) {
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
            index = (index - 1 + headlines.size()) % headlines.size();
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
        return TickerTouchHandler.isDragExceeded(downX, downY, currentX, currentY, touchSlop);
    }

    public static boolean isValidTap(boolean isClickValid, long duration, int longPressTimeout) {
        return TickerTouchHandler.isValidTap(isClickValid, duration, longPressTimeout);
    }

    public static boolean shouldFillGap(float maxRight, int containerWidth) {
        return maxRight < containerWidth;
    }

    public static boolean shouldFillLeftGap(float minLeft) {
        return minLeft > 0;
    }

    private TextView getTextView(boolean animationsEnabled, String headline) {
        return getTextView(animationsEnabled, headline, animationController.getIndex());
    }

    private TextView getTextView(boolean animationsEnabled, String headline, int itemIndex) {
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
        tv.setTag(itemIndex);
        tv.setTextColor(animationController.getTextColor());
        tv.setTextSize(animationController.getTextSize());
        if (!headline.isEmpty()) {
            tv.setText(headline);
        }
        return tv;
    }

    void removeNonAnimatedViews() {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View view = getChildAt(i);
            if (!animationController.isViewAnimated(view)) {
                removeViewAt(i);
            }
        }
    }

    private void animationStart(final TextView tv) {
        if (animationController.isViewAnimated(tv)) {
            return;
        }

        tv.setTranslationX(getWidth());
        int distance = (int) tv.getPaint().measureText(tv.getText().toString());
        tv.animate()
                .translationXBy(-distance)
                .setDuration(distance * animationController.getAnimationSpeed())
                .setInterpolator(new LinearInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(@NonNull Animator animation) {
                        Log.d(TAG, "start animation");
                        animationController.addAnimator(animation);
                        animationController.addAnimatedView(tv);
                    }

                    @Override
                    public void onAnimationEnd(@NonNull Animator animation) {
                        animationController.removeAnimator(animation);
                        if (tv.getParent() != null) {
                            animationEnd(tv);
                            launchNext();
                        }
                    }

                    @Override
                    public void onAnimationCancel(@NonNull Animator animation) {
                        animationController.removeAnimator(animation);
                        animationController.removeAnimatedView(tv);
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
                .setDuration(distance * animationController.getAnimationSpeed())
                .setInterpolator(new LinearInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(@NonNull Animator animation) {
                        animationController.addAnimator(animation);
                    }

                    @Override
                    public void onAnimationEnd(@NonNull Animator animation) {
                        removeView(tv);
                        animationController.removeAnimator(animation);
                        animationController.removeAnimatedView(tv);
                    }

                    @Override
                    public void onAnimationCancel(@NonNull Animator animation) {
                        animationController.removeAnimatedView(tv);
                        animationController.removeAnimator(animation);
                    }

                    @Override
                    public void onAnimationRepeat(@NonNull Animator animation) {
                    }
                })
                .start();
    }
}
