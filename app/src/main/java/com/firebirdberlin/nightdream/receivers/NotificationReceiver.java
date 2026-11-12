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

package com.firebirdberlin.nightdream.receivers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.databinding.DataBindingUtil;

import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.NotificationList.NotificationApp;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.databinding.NotificationMediacontrolBinding;
import com.firebirdberlin.nightdream.events.AppEventBus;
import com.firebirdberlin.nightdream.events.OnMediaNotificationEvent;
import com.firebirdberlin.nightdream.events.OnNotificationAppsUpdated;
import com.firebirdberlin.nightdream.events.OnNotificationPreviewEvent;
import com.firebirdberlin.nightdream.ui.ClockLayout;
import com.firebirdberlin.nightdream.ui.MediaControlLayout;
import com.firebirdberlin.nightdream.ui.NotificationPreviewLayout;
import com.google.android.flexbox.FlexboxLayout;

import java.util.List;
import java.util.function.Consumer;

public class NotificationReceiver {
    private static final String TAG = NotificationReceiver.class.getSimpleName();
    private final View contentView;
    private final NightDreamActivity activity;
    private final Consumer<Object> eventListener;
    private int color;

    public NotificationReceiver(NightDreamActivity context) {
        Window window = context.getWindow();
        contentView = window.getDecorView().findViewById(android.R.id.content);
        activity = context;
        eventListener = event -> {
            if (event instanceof OnNotificationAppsUpdated appsEvent) {
                handleNotificationApps(activity, appsEvent.notificationApps);
            } else if (event instanceof OnNotificationPreviewEvent previewEvent) {
                handleNotificationPreview(activity, previewEvent.action, previewEvent.intent);
            } else if (event instanceof OnMediaNotificationEvent mediaEvent) {
                handleMediaNotification(activity, mediaEvent.action, mediaEvent.intent);
            }
        };
    }

    public void register() {
        AppEventBus.addListener(eventListener);
    }

    public void unregister() {
        AppEventBus.removeListener(eventListener);
    }

    public static void dumpIntent(Intent i) {
        if (i == null) return;
        Bundle bundle = i.getExtras();
        if (bundle == null) return;
        for (String key : bundle.keySet()) {
            @SuppressWarnings("deprecation")
            Object value = bundle.get(key);
            if (value != null) {
                Log.d(TAG, String.format("%s %s (%s)", key, value, value.getClass().getName()));
            }
        }
    }

    public void setColor(int color) {
        this.color = color;
    }

    private void handleNotificationApps(final Context context, List<NotificationApp> notificationApps) {
        if (context == null) return;

        boolean showNotification = Settings.showNotification(context);
        if (!showNotification) {
            removeViewsFrom(R.id.notificationstatusbar);
            removeViewsFrom(R.id.notificationbar);
            return;
        }

        int notificationResId = Settings.getNotificationContainerResourceId(context);
        FlexboxLayout container = contentView.findViewById(notificationResId);
        if (container == null) {
            return;
        }

        removeViewsFrom(R.id.notificationstatusbar);
        removeViewsFrom(R.id.notificationbar);
        if (Utility.isEmpty(notificationApps)) return;
        for (NotificationApp app : notificationApps) {
            if (app == null) continue;
            Log.d(TAG, app.getName() + ": " + app.getIconId());
            Drawable icon = getIcon(context, app);
            addNotificationIcon(context, container, icon);
        }
    }

    private void handleNotificationPreview(final Context context, String action, Intent intent) {
        if (context == null) return;

        boolean showNotification = Settings.showNotification(context);
        if (!showNotification || "clear".equals(action)) {
            removeViewsFrom(R.id.notificationstatusbar);
            removeViewsFrom(R.id.notificationbar);
            return;
        }

        if ("added_preview".equals(action) && intent != null) {
            setupNotificationPreview(context, intent);
        }
    }

    private void handleMediaNotification(final Context context, String action, Intent intent) {
        if (context == null) return;

        if ("added_media".equals(action) && intent != null) {
            setupMediaControls(context, intent);
        } else if ("removed_media".equals(action)) {
            removeMediaControls();
        }
    }

    private Drawable getIcon(Context context, NotificationApp notificationApp) {
        Bitmap bitmap = notificationApp.getSmallNotificationIcon();
        if (bitmap != null) {
            return new BitmapDrawable(context.getResources(), bitmap);
        }
        return ContextCompat.getDrawable(context, R.drawable.ic_info);
    }

    private void removeViewsFrom(int resId) {
        FlexboxLayout container = contentView.findViewById(resId);
        if (container != null) {
            container.removeAllViews();
        }
    }

    private void addNotificationIcon(Context context, FlexboxLayout container, Drawable icon) {
        ImageView image = new ImageView(context);
        int padding = Utility.dpToPx(context, 2);
        image.setPadding(padding, padding, padding, padding);
        image.setImageDrawable(icon);
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

    private Drawable getNotificationIcon(Context context, Intent intent) {
        Bitmap smallIcon =
                IntentCompat.getParcelableExtra(intent, "smallIconBitmap", Bitmap.class);
        return new BitmapDrawable(context.getResources(), smallIcon);
    }

    private void removeMediaControls() {
        ConstraintLayout mediaStyleContainer =
                contentView.findViewById(R.id.notification_mediacontrol_bar);

        if (mediaStyleContainer != null) {
            mediaStyleContainer.removeAllViews();
            mediaStyleContainer.setVisibility(View.GONE);
            ClockLayout clockLayout = contentView.findViewById(R.id.clockLayout);
            if (clockLayout != null) {
                clockLayout.updateLayout();
            }
        }
    }

    private void setupMediaControls(Context context, Intent intent) {
        if (!Settings.showMediaStyleNotification(context) || activity == null) {
            return;
        }

        String template = intent.getStringExtra("template");
        if (template == null || !template.contains("MediaStyle")) {
            return;
        }

        Log.i(TAG, "Show MediaStyle notification");

        Drawable notificationMessageSmallIcon = getNotificationIcon(context, intent);

        ConstraintLayout mediaStyleContainer =
                contentView.findViewById(R.id.notification_mediacontrol_bar);

        if (mediaStyleContainer == null) {
            return;
        }

        View boundView = mediaStyleContainer.getChildAt(0);
        NotificationMediacontrolBinding binding =
                boundView != null ? DataBindingUtil.getBinding(boundView) : null;

        MediaControlLayout model;

        if (binding != null) {
            model = binding.getModel();
        } else {
            model = new MediaControlLayout(activity, mediaStyleContainer);

            mediaStyleContainer.removeAllViews();
            mediaStyleContainer.addView(model.getView());
        }

        model.setColor(color);
        model.setupFromNotificationIntent(context, intent, notificationMessageSmallIcon);
        mediaStyleContainer.setVisibility(View.VISIBLE);
        ClockLayout clockLayout = contentView.findViewById(R.id.clockLayout);
        if (clockLayout != null) {
            clockLayout.updateLayout();
        }
    }

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
