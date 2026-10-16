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
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.google.android.flexbox.FlexboxLayout;

public class SidePanel extends FlexboxLayout {
    private static final String TAG = "SidePanel";
    private final int transparentColor = getResources().getColor(android.R.color.transparent);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int mIconBackground;
    private Interpolator mAnimationInterpolator;
    private int mAnimationDuration = 250;
    private boolean menuIsOpen = false;
    private boolean mLockedCLick;
    private final Runnable hideSideMenu = new Runnable() {
        @Override
        public void run() {
            removeCallbacks(hideSideMenu);
            closeMenu();
        }
    };
    private int mIconColor;
    private int mAccentColor;
    private int mSide = 0; // 0 for left, 1 for right

    public SidePanel(Context context) {
        super(context);
        init(context, null);
    }

    public SidePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SidePanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private static void setIconSize(Context context, ImageView icon) {
        int dim = Utility.dpToPx(context, 48);
        icon.getLayoutParams().height = dim;
        icon.getLayoutParams().width = dim;
    }

    @Override
    protected void onFinishInflate() {
        Log.d(TAG, "onFinishInflate");
        super.onFinishInflate();
    }

    private void init(Context context, AttributeSet attrs) {
        Log.d(TAG, "init");

        mAnimationInterpolator = new AccelerateDecelerateInterpolator();
        mIconColor = transparentColor;

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SidePanel, 0, 0);
            try {
                mIconColor = a.getColor(R.styleable.SidePanel_iconColor, transparentColor);
                mIconBackground = a.getResourceId(R.styleable.SidePanel_iconBackground, 0);
                mSide = a.getInt(R.styleable.SidePanel_side, 0);
            } finally {
                a.recycle();
            }
        } else {
            this.setBackground(null);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        post(() -> setTranslationX(mSide == 0 ? -getWidth() : getWidth()));
        menuIsOpen = false;
        handler.removeCallbacks(hideSideMenu);
        colorizeIcons();
        setIconBackgroundResource(mIconBackground);
    }

    public void closeMenu() {
        Log.d(TAG, "closeMenu(): " + menuIsOpen);
        if (menuIsOpen) {
            ObjectAnimator animation;
            if (mSide == 0) {
                animation = ObjectAnimator.ofFloat(
                        this, "translationX", 0, -this.getWidth()
                );
            } else {
                animation = ObjectAnimator.ofFloat(
                        this, "translationX", 0, this.getWidth()
                );
            }
            startMenuAnimation(animation);
            handler.removeCallbacks(hideSideMenu);
        }
    }

    public void openMenu() {
        Log.d(TAG, "openMenu(): " + menuIsOpen);
        setVisibility(VISIBLE);
        if (!menuIsOpen) {
            ObjectAnimator animation;
            if (mSide == 0) {
                animation = ObjectAnimator.ofFloat(
                        this, "translationX", -this.getWidth(), 0
                );
            } else {
                animation = ObjectAnimator.ofFloat(
                        this, "translationX", this.getWidth(), 0
                );
            }
            startMenuAnimation(animation);
            handler.postDelayed(hideSideMenu, 20000);
        }
    }

    private void startMenuAnimation(ObjectAnimator animation) {
        Log.d(TAG, "startMenuAnimation()");
        animation.setInterpolator(mAnimationInterpolator);
        animation.setDuration(mAnimationDuration);
        animation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mLockedCLick = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                menuIsOpen = !menuIsOpen;
                mLockedCLick = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animation.start();
    }

    //Sets background drawable for all icons
    public SidePanel setIconBackgroundResource(int iconBackground) {
        mIconBackground = iconBackground;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.setBackground(mIconBackground != 0 ? ContextCompat.getDrawable(getContext(), mIconBackground) : null);
        }
        return this;
    }

    //Gets the Icon background
    public int getIconBackground() {
        return mIconBackground;
    }

    // Sets the background for the menu
    public SidePanel setMenuBackground(Drawable menuBackground) {
        this.setBackground(menuBackground);
        return this;
    }

    // Sets animation interpolator
    public SidePanel setAnimationInterpolator(Interpolator animationInterpolator) {
        this.mAnimationInterpolator = animationInterpolator;
        return this;
    }

    // Sets the animation duration
    public SidePanel setAnimationDuration(int animationDuration) {
        this.mAnimationDuration = animationDuration;
        return this;
    }

    //sets color to icons
    private void colorizeIcons() {
        Log.d(TAG, "colorizeIcons()");
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view instanceof ImageView) {
                ImageView iv = (ImageView) view;
                applyMetallicGradientToIcon(iv);
                setIconSize(getContext(), iv);
            }
        }
    }

    private void applyMetallicGradientToIcon(ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        if (drawable == null) return;

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        if (width <= 0) width = 96;
        if (height <= 0) height = 96;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        int baseColor = mIconColor != 0 ? mIconColor : Color.rgb(229, 193, 88);
        int highlightColor = lightenColor(baseColor, 0.45f);
        int shadowColor = darkenColor(baseColor, 0.4f);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        LinearGradient shader = new LinearGradient(
                0, 0, width, height,
                new int[]{highlightColor, baseColor, shadowColor},
                new float[]{0.0f, 0.5f, 1.0f},
                Shader.TileMode.CLAMP
        );
        paint.setShader(shader);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawRect(0, 0, width, height, paint);

        imageView.setImageBitmap(bitmap);
    }

    private int lightenColor(int color, float factor) {
        int r = Math.min(255, (int) (Color.red(color) + (255 - Color.red(color)) * factor));
        int g = Math.min(255, (int) (Color.green(color) + (255 - Color.green(color)) * factor));
        int b = Math.min(255, (int) (Color.blue(color) + (255 - Color.blue(color)) * factor));
        return Color.rgb(r, g, b);
    }

    private int darkenColor(int color, float factor) {
        int r = (int) (Color.red(color) * (1f - factor));
        int g = (int) (Color.green(color) * (1f - factor));
        int b = (int) (Color.blue(color) * (1f - factor));
        return Color.rgb(r, g, b);
    }

    public boolean isHidden() {
        return !menuIsOpen;
    }

    public void setTorchIconActive(boolean on) {
        ImageView torchIcon = findViewById(R.id.flashlight_icon);
        if (on) {
            setIconActive(torchIcon);
        } else {
            setIconInactive(torchIcon);
        }
    }

    public void setTorchIconVisibility(boolean visible) {
        ImageView torchIcon = findViewById(R.id.flashlight_icon);
        if (visible) {
            torchIcon.setVisibility(VISIBLE);
        } else {
            torchIcon.setVisibility(GONE);
        }
    }

    public void setRadioIconActive(boolean on) {
        ImageView radioIcon = findViewById(R.id.radio_icon);
        if (on) {
            setIconActive(radioIcon);
        } else {
            setIconInactive(radioIcon);
        }
    }

    private void setIconActive(ImageView icon) {
        post(() -> {
            icon.setColorFilter(mAccentColor, PorterDuff.Mode.SRC_ATOP);
            setIconSize(getContext(), icon);
        });
    }

    private void setIconInactive(ImageView icon) {
        post(() -> {
            icon.setColorFilter(mIconColor, PorterDuff.Mode.SRC_ATOP);
            setIconSize(getContext(), icon);
        });
    }

    public void setAccentColor(int accentColor) {
        this.mAccentColor = accentColor;
    }

    public void setSecondaryColor(int iconColor) {
        this.mIconColor = iconColor;
        colorizeIcons();
    }

    public void setPaddingLeft(int padding) {
        setPadding(padding, getPaddingTop(), getPaddingRight(), getPaddingBottom());
        requestLayout();
        post(() -> setTranslationX(isHidden() ? (mSide == 0 ? -getWidth() : getWidth()) : 0));
    }

    public void setPaddingRight(int padding) {
        setPadding(getPaddingLeft(), getPaddingTop(), padding, getPaddingBottom());
        requestLayout();
        post(() -> setTranslationX(isHidden() ? (mSide == 0 ? -getWidth() : getWidth()) : 0));
    }
}
