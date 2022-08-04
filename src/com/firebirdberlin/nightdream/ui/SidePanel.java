package com.firebirdberlin.nightdream.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
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
    private View paddingLeft;

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

        paddingLeft = findViewById(R.id.padding_left);

        setX(getWidth());
        menuIsOpen = false;
        handler.removeCallbacks(hideSideMenu);
        colorizeIcons();
        setIconBackgroundResource(mIconBackground);
    }

    public void toggleMenu() {
        Log.d(TAG, "toggleMenu()");
        // Prevents opening and closing frenetically the menu
        if (mLockedCLick) {
            return;
        }

        if (menuIsOpen) {
            closeMenu();
        } else {
            openMenu();
        }
    }

    public void closeMenu() {
        Log.d(TAG, "closeMenu(): " + menuIsOpen);
        if (menuIsOpen) {
            ObjectAnimator animation;
            animation = ObjectAnimator.ofFloat(
                    this, "translationX", 0, -this.getWidth()
            );
            startMenuAnimation(animation);
            handler.removeCallbacks(hideSideMenu);
        }
    }

    public void openMenu() {
        Log.d(TAG, "openMenu(): " + menuIsOpen);
        if (!menuIsOpen) {
            ObjectAnimator animation;
            animation = ObjectAnimator.ofFloat(
                    this, "translationX", -this.getWidth(), 0
            );
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
                ((ImageView) view).setColorFilter(mIconColor, PorterDuff.Mode.SRC_ATOP);
            }
        }
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
        paddingLeft.getLayoutParams().width = padding;
        requestLayout();
        post(() -> setX(isHidden() ? -getWidth() : 0));
    }
}
