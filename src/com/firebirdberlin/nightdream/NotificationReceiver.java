package com.firebirdberlin.nightdream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;

import com.firebirdberlin.nightdream.NotificationList.NotificationApp;
import com.firebirdberlin.nightdream.databinding.NotificationMediacontrolBinding;
import com.firebirdberlin.nightdream.ui.MediaControlLayout;
import com.firebirdberlin.nightdream.ui.NotificationPreviewLayout;
import com.firebirdberlin.nightdream.viewmodels.NotificationViewModel;

import java.util.List;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    private final View contentView;
    private final NightDreamActivity activity;
    private int color;

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
            e.printStackTrace();
            Log.e(TAG, "Unknown error in onReceive (NotificationReceiver)");
        }
    }

    private void handleOnReceive(final Context context, Intent intent) {

        if (intent == null || context == null) return;
        if (Utility.isDebuggable(context)) {
            Log.d(TAG, "Broadcast received.");
            dumpIntent(intent);
        }
        String action = intent.getStringExtra("action");
        Log.d(TAG, "action:" + action);

        boolean showNotification = Settings.showNotification(context);
        Log.d(TAG, "showNotification: " + showNotification);

        if (!showNotification || "clear".equals(action)) {
            return;
        }

        Log.d(TAG, "action:" + action);
        if (action != null) {
            switch (action) {
                case "scan":
                    List<NotificationApp> notificationApps = intent.getParcelableArrayListExtra("notificationApps");
                    if (notificationApps == null) break;
                    NotificationViewModel.setNotificationApp(notificationApps);
                    break;
                case "added_media":
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        setupMediaControls(context, intent);
                    }
                    break;
                case "removed_media":
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        removeMediaControls();
                    }
                    break;
                case "added_preview":
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        setupNotificationPreview(context, intent);
                    }
                    break;
                default:
            }
        }
    }

    private Drawable getNotificationIcon(Context context, Intent intent) {
        Bitmap smallIcon = intent.getParcelableExtra("smallIconBitmap");
        return new BitmapDrawable(context.getResources(), smallIcon);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void removeMediaControls() {
        ConstraintLayout mediaStyleContainer = contentView.findViewById(R.id.notification_mediacontrol_bar);

        if (mediaStyleContainer != null) {
            mediaStyleContainer.removeAllViews();
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

        Drawable notificationMessageSmallIcon = getNotificationIcon(context, intent);

        ConstraintLayout mediaStyleContainer = contentView.findViewById(R.id.notification_mediacontrol_bar);

        if (mediaStyleContainer != null) {
            View boundView = mediaStyleContainer.getChildAt(0);
            NotificationMediacontrolBinding mediaControlLayoutBinding = DataBindingUtil.getBinding(boundView);

            if (mediaControlLayoutBinding != null) {
                mediaControlLayoutBinding.getModel().setupFromNotificationIntent(context, intent, notificationMessageSmallIcon);
            } else {
                MediaControlLayout mediaControlLayout = new MediaControlLayout(mediaStyleContainer);
                mediaStyleContainer.removeAllViews();
                mediaStyleContainer.addView(mediaControlLayout.getView());
                mediaControlLayout.setupFromNotificationIntent(context, intent, notificationMessageSmallIcon);
                boundView = mediaStyleContainer.getChildAt(0);
                mediaControlLayoutBinding = DataBindingUtil.getBinding(boundView);
                mediaControlLayoutBinding.getModel().setColor(color);
            }
            mediaControlLayoutBinding.invalidateAll();
        }

        View clockLayout = contentView.findViewById(R.id.clockLayout);
        clockLayout.postDelayed(
                () -> activity.onConfigurationChanged(activity.getResources().getConfiguration()),
                500
        );
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void setupNotificationPreview(Context context, Intent intent) {
        if (!Settings.showNotificationPreview(context) || activity == null) {
            return;
        }

        String template = intent.getStringExtra("template");
        if (Utility.contains(template, "MediaStyle")) {
            return;
        }

        Drawable notificationMessageSmallIcon = getNotificationIcon(context, intent);
        NotificationPreviewLayout container = contentView.findViewById(R.id.notification_preview);
        container.setupFromNotificationIntent(context, intent, notificationMessageSmallIcon);
    }
}
