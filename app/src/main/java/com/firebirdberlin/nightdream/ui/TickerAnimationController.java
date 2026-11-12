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
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.RssFeedItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TickerAnimationController {
    private static final String TAG = TickerAnimationController.class.getSimpleName();

    private long animationSpeed = 5L;
    private final List<String> headlines = new ArrayList<>();
    private final List<String> urls = new ArrayList<>();
    private final Set<View> animatedViews = new HashSet<>();
    private final Set<Animator> animators = new HashSet<>();
    private int textColor = Color.WHITE;
    private int index = -1;
    private float textSize = 28.f;
    private boolean running = false;
    private boolean isPaused = false;
    private final Context context;
    private final Handler handler;

    public TickerAnimationController(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public boolean isAnimationActive() {
        return Utility.isPlugged(context) && Utility.areSystemAnimationsEnabled(context);
    }

    public void setTickerSpeed(Long speed) {
        if (speed != null && animationSpeed != speed) {
            animationSpeed = speed;
        }
    }

    public long getAnimationSpeed() {
        return animationSpeed;
    }

    public List<String> getHeadlines() {
        return headlines;
    }

    public List<String> getUrls() {
        return urls;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void cancelAnimations() {
        for (Animator animator : new ArrayList<>(animators)) {
            animator.cancel();
        }
        animators.clear();
        animatedViews.clear();
    }

    public void pauseAnimations() {
        for (Animator animator : new ArrayList<>(animators)) {
            animator.pause();
        }
    }

    public void resumeAnimations() {
        for (Animator animator : new ArrayList<>(animators)) {
            animator.resume();
        }
    }

    public void addAnimator(Animator animator) {
        animators.add(animator);
    }

    public void removeAnimator(Animator animator) {
        animators.remove(animator);
    }

    public void addAnimatedView(View view) {
        animatedViews.add(view);
    }

    public void removeAnimatedView(View view) {
        animatedViews.remove(view);
    }

    public boolean isViewAnimated(View view) {
        return animatedViews.contains(view);
    }

    public void clearAnimatedViews() {
        animatedViews.clear();
    }

    public void setHeadlines(List<RssFeedItem> items) {
        if (items != null && !items.isEmpty()) {
            headlines.clear();
            urls.clear();
            int count = 0;
            for (RssFeedItem item : items) {
                String title = item.getTitle();
                String link = item.getLink();
                headlines.add(title);
                urls.add(link);
                if (++count >= 15) {
                    break;
                }
            }
        }
    }

    public void addHeadline(String headline) {
        headlines.add(headline);
    }

    public void clear() {
        headlines.clear();
        urls.clear();
        animators.clear();
        animatedViews.clear();
        index = -1;
        running = false;
        isPaused = false;
        handler.removeCallbacksAndMessages(null);
    }
}
