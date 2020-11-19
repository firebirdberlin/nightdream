package com.firebirdberlin.nightdream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.firebirdberlin.nightdream.databinding.NotificationMediacontrolBinding;
import com.firebirdberlin.nightdream.ui.ExifView;
import com.firebirdberlin.nightdream.ui.MediaControlLayout;
import com.firebirdberlin.nightdream.ui.NightDreamUI;
import com.google.android.flexbox.FlexboxLayout;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    private int color;
    private View contentView;
    private NightDreamActivity activity;

    public NotificationReceiver(NightDreamActivity context) {
        Window window = context.getWindow();
        contentView = window.getDecorView().findViewById(android.R.id.content);
        activity = context;
    }

    public static void dumpIntent(Intent i) {
        Bundle bundle = i.getExtras();
        if (bundle == null) return;
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            if (value != null) {
                Log.d(TAG, String.format("%s %s (%s)", key, value.toString(), value.getClass().getName()));
            }
        }
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            handleOnReceive(context, intent);
        } catch (NullPointerException e) {
            Log.e(TAG, "Unknown error in onReceive (NotificationReceiver)");
        }
    }

    private void handleOnReceive(final Context context, Intent intent) {

        if (intent == null || context == null) return;
        if (Utility.isDebuggable(context)) {
            Log.d(TAG, "Broadcast received.");
            dumpIntent(intent);
        }

        boolean showNotification = Settings.showNotification(context);
        Log.d(TAG, "showNotification: " + showNotification);
        FlexboxLayout notificationBar = contentView.findViewById(R.id.notificationbar);
        FlexboxLayout notificationStatusBar = contentView.findViewById(R.id.notificationstatusbar);

        if (!showNotification) {
            removeViewsFrom(notificationBar);
            removeViewsFrom(notificationStatusBar);
            return;
        }

        FlexboxLayout container =
                Settings.useNotificationStatusBar(context) ? notificationStatusBar : notificationBar;
        if (container == null) {
            return;
        }

        String action = intent.getStringExtra("action");

        if ("clear".equals(action)) {
            removeViewsFrom(notificationBar);
            removeViewsFrom(notificationStatusBar);
            return;
        }

        String packageName = intent.getStringExtra("packageName");
        if (packageName == null || packageName.isEmpty()) {
            return;
        }

        int iconId = intent.getIntExtra("iconId", -1);
        Drawable icon = getNotificationIcon(context, packageName, iconId);
        if (icon == null) {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_info);
        }

        if ("added".equals(action) && icon != null) {
            addNotificationIcon(context, container, icon);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                setupMediaControls(context, intent);
            }
        }

    }

    private void removeViewsFrom(FlexboxLayout layout) {
        if (layout != null) layout.removeAllViews();
    }

    private void addNotificationIcon(Context context, FlexboxLayout container, Drawable icon) {
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
        container.addView(image);
    }

    private Drawable getNotificationIcon(Context context, String packageName, int id) {
        Log.d(TAG, "getNotificationIcon for id = " + id);
        if (packageName == null || id == -1) {
            return null;
        }
        try {
            Context remotePackageContext = context.getApplicationContext().createPackageContext(packageName, 0);
            return ContextCompat.getDrawable(remotePackageContext, id);
        } catch (NullPointerException | NameNotFoundException | Resources.NotFoundException e) {
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void setupMediaControls(Context context, Intent intent) {
        if (!Settings.showMediaStyleNotification(context) || activity == null) {
            return;
        }
        // TODO: media control area is too wide
        // TODO: adjust the text color of the media controls according to the secondary text color
        String template = intent.getStringExtra("template");
        if (template == null || !template.contains("MediaStyle")) {
            return;
        }
        Log.i(TAG, "Show MediaStyle notification");
        Log.i(TAG, "template = " + template);

        int iconId = intent.getIntExtra("iconId", -1);
        String packageName = intent.getStringExtra("packageName");
        Drawable notificationMessageSmallIcon = getNotificationIcon(context, packageName, iconId);
        View clockLayout = contentView.findViewById(R.id.clockLayout);

        ConstraintLayout mediaStyleContainer = contentView.findViewById(R.id.notification_mediacontrol_bar);

        if (mediaStyleContainer != null) {
            View boundView = mediaStyleContainer.getChildAt(0);
            NotificationMediacontrolBinding mediaControlLayoutBinding = DataBindingUtil.getBinding(boundView);

            if (mediaControlLayoutBinding != null) {
                mediaControlLayoutBinding.getModel().setupFromNotificationIntent(context, intent, notificationMessageSmallIcon);
                mediaControlLayoutBinding.invalidateAll();
            } else {
                MediaControlLayout mediaControlLayout = new MediaControlLayout(mediaStyleContainer);
                mediaStyleContainer.removeAllViews();
                mediaStyleContainer.addView(mediaControlLayout.getView());
                mediaControlLayout.setupFromNotificationIntent(context, intent, notificationMessageSmallIcon);
            }
        }

        clockLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                activity.onConfigurationChanged(activity.getResources().getConfiguration());
            }
        }, 500);
    }
}
