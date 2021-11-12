package com.firebirdberlin.nightdream.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.repositories.FlashlightProvider;
import com.google.android.flexbox.FlexboxLayout;

public class SideMenu extends ConstraintLayout {
    private static final String TAG = "SideMenu";

    private int mIconBackground;
    private Interpolator mAnimationInterpolator;
    private int mAnimationDuration = 250;
    private final int transparentColor = getResources().getColor(android.R.color.transparent);
    private boolean menuIsOpen = false;
    private boolean mLockedCLick;
    private int mIconColor;
    private int mAccentColor;
    private Context context;
    private View paddingLeft;
    private FlexboxLayout sidePanel;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable hideSideMenu = new Runnable() {
        @Override
        public void run() {
            removeCallbacks(hideSideMenu);
            closeMenu();
        }
    };

    public SideMenu(Context context) {
        super(context);
        init(context, null);
    }

    public SideMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SideMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG,"onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onFinishInflate() {
        Log.d(TAG,"onFinishInflate");
        super.onFinishInflate();
    }

    //Initialize view and attributes from xml
    private void init(Context context, AttributeSet attrs) {
        Log.d(TAG, "init");

        this.context = context;

        mAnimationInterpolator = new AccelerateDecelerateInterpolator();
        mIconColor = transparentColor;

        //xml attributes
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SideMenu, 0, 0);
            try {
                mIconColor = a.getColor(R.styleable.SideMenu_SideMenuIconColor, transparentColor);
                mIconBackground = a.getResourceId(R.styleable.SideMenu_SideMenuIconBackground, 0);
                int backgroundResource = a.getResourceId(R.styleable.SideMenu_SideMenuBackground, 0);
                Drawable menuBackground = backgroundResource != 0 ? ContextCompat.getDrawable(context, backgroundResource) : null;
                this.setBackground(menuBackground);
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
        sidePanel = findViewById(R.id.side_panel);

        ((ViewGroup) getParent()).setClipChildren(false);
        ((ViewGroup) getParent()).setClipToPadding(false);

//        getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
        getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;

        initMenu();
        colorizeIcons();
        setIconBackgroundResource(mIconBackground);
    }

    private void initMenu(){
        setX(-1000f);
        menuIsOpen = false;
        handler.removeCallbacks(hideSideMenu);
    }

    //open and close menu
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
            animation = ObjectAnimator.ofFloat(this, "translationX", 0, -this.getWidth());
            startMenuAnimation(animation);
            handler.removeCallbacks(hideSideMenu);
        }
    }

    public void openMenu() {
        Log.d(TAG, "openMenu(): " + menuIsOpen);
        if (!menuIsOpen) {
            ObjectAnimator animation;
            animation = ObjectAnimator.ofFloat(this, "translationX", -this.getWidth(), 0);
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
    public SideMenu setIconBackgroundResource(int iconBackground) {
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
    public SideMenu setMenuBackground(Drawable menuBackground) {
        this.setBackground(menuBackground);
        return this;
    }

    // Sets animation interpolator
    public SideMenu setAnimationInterpolator(Interpolator animationInterpolator) {
        this.mAnimationInterpolator = animationInterpolator;
        return this;
    }

    // Sets the animation duration
    public SideMenu setAnimationDuration(int animationDuration) {
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

    public boolean sidePanelIsHidden() {
        return !menuIsOpen;
    }

    public void setIconActive(ImageView icon) {
        post(() -> {
            icon.setColorFilter(mAccentColor, PorterDuff.Mode.SRC_ATOP);
            Utility.setIconSize(getContext(), icon);
        });
    }

    public void setIconInactive(ImageView icon) {
        post(() -> {
            icon.setColorFilter(mIconColor, PorterDuff.Mode.SRC_ATOP);
            Utility.setIconSize(getContext(), icon);
        });
    }

    public void setAccentColor(int accentColor) {
        this.mAccentColor = accentColor;
    }

    public void setSecondaryColor(int iconColor) {
        this.mIconColor = iconColor;
        colorizeIcons();
    }

    public void setupFlashlight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setupFlashlight(new FlashlightProvider(getContext()));
        }
    }

    public void setPaddingLeft(int padding){
        Log.d(TAG, "setPaddingLeft");
        initMenu();
        paddingLeft.getLayoutParams().width = padding;
    }

    //setup flashlight icon
    public void setupFlashlight(FlashlightProvider flash) {
        Log.d(TAG, "setupFlashlight");
        ImageView torchIcon;

        torchIcon = findViewById(R.id.torch_icon);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (flash == null) {
                flash = new FlashlightProvider(getContext());
            }
            torchIcon.setVisibility(flash.hasCameraFlash() ? View.VISIBLE : View.GONE);
            if (flash.isFlashlightOn()) {
                Log.d(TAG, "Flashlight is ON");
                setIconActive(torchIcon);
            } else {
                Log.d(TAG, "Flashlight is OFF");
                setIconInactive(torchIcon);
            }
        } else {
            torchIcon.setVisibility(View.GONE);
        }
    }
}
