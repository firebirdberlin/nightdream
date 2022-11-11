package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.firebirdberlin.nightdream.NotificationList.NotificationApp;
import com.firebirdberlin.nightdream.NotificationListActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.viewmodels.NotificationViewModel;
import com.google.android.flexbox.FlexboxLayout;

public class NotificationBar extends FlexboxLayout {
    private static final String TAG = "NotificationBar";
    private Context context;
    private int color;
    int resourceID;
    boolean showNotification = false;

    //Constructor
    public NotificationBar(@NonNull Context context) {
        super(context);
    }

    public NotificationBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        Log.d(TAG, "Constructor");

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.notification_bar, this);

        Settings settings = new Settings(context);
        this.showNotification = Settings.showNotification(context);
        this.resourceID = Settings.getNotificationContainerResourceId(context);

        if (showNotification) {
            switchNotification();
        }

        setOnClickListener(view -> {
            Log.d(TAG, "onClick");
            if (settings.isUIlocked) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                if (getChildCount() > 0) {
                    NotificationListActivity.start(context);
                }
            }
        });

        NotificationViewModel.observeNotificationApp(context, notificationApps -> {
            if (notificationApps != null) {
                removeAllViews();
                for (NotificationApp app : notificationApps) {
                    Log.d(TAG, app.getName() + ": " + app.getIconId());
                    Drawable icon = getIcon(context, app);
                    addNotificationIcon(context, icon);
                }
            }
        });

        NotificationViewModel.observeResourceId(context, resourceId -> {
            if (resourceId != null) {
                this.resourceID = resourceId;
                if (showNotification) {
                    switchNotification();
                }
            }
        });

        NotificationViewModel.observeShowNotification(context, showNotification -> {
            if (showNotification != null) {
                this.showNotification = showNotification;
                if (!showNotification){
                    setVisibility(View.GONE);
                }
                else {
                    switchNotification();
                }
            }
        });

        NotificationViewModel.observeTextColor(context, textColor -> {
            this.color = textColor;
            Utility.colorizeView(this, textColor);
        });
    }

    private void switchNotification(){
        if (this.resourceID == getId()) {
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }
    }

    private Drawable getIcon(Context context, NotificationApp notificationApp) {
        Bitmap bitmap = notificationApp.getSmallNotificationIcon();
        Drawable icon = null;
        if (bitmap != null) {
            icon = new BitmapDrawable(context.getResources(), bitmap);
        }
        if (icon == null) {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_info);
        }
        return icon;
    }

    private void addNotificationIcon(Context context, Drawable icon) {
        ImageView image = new ImageView(context);
        int padding = Utility.dpToPx(context, 5);
        image.setPadding(padding, 0, 0, 0);
        image.setImageDrawable(icon);
        image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setFlexShrink(0.25f);
        layoutParams.setFlexGrow(0.f);
        layoutParams.setHeight(Utility.dpToPx(context, 24.f));
        layoutParams.setMaxWidth(Utility.dpToPx(context, 24.f));
        layoutParams.setMaxHeight(Utility.dpToPx(context, 24.f));
        image.setLayoutParams(layoutParams);
        addView(image);
    }

}
