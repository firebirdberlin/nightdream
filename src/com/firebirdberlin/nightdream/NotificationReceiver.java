package com.firebirdberlin.nightdream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.google.android.flexbox.FlexboxLayout;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    private int color;
    private View contentView;


    public NotificationReceiver(NightDreamActivity context) {
        Window window = context.getWindow();
        contentView = window.getDecorView().findViewById(android.R.id.content);
    }

    public NotificationReceiver(Window window) {
        contentView = window.getDecorView().findViewById(android.R.id.content);
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (Utility.isDebuggable(context)){
            Log.d(TAG, "Broadcast received.");
            dumpIntent(intent);
        }

        FlexboxLayout notificationBar = contentView.findViewById(R.id.notificationbar);
        FlexboxLayout notificationStatusBar = contentView.findViewById(R.id.notificationstatusbar);
        FlexboxLayout container = (Settings.useNotificationStatusBar(context)) ?
                notificationStatusBar : notificationBar;
        if (container == null) {
            return;
        }

        String action = intent.getStringExtra("action");
        action = (action == null) ? "" : action;

        if (action.equals("clear")) {
            notificationBar.removeAllViews();
            notificationStatusBar.removeAllViews();
            return;
        }

        String packageName = intent.getStringExtra("packageName");
        int iconId = intent.getIntExtra("iconId", -1);
        Drawable icon = getNotificationIcon(context, packageName, iconId);
        if (action.equals("added") && icon != null) {
            AppCompatImageView image = new AppCompatImageView(context);
            int padding = Utility.dpToPx(context, 5);
            image.setPadding(padding, 0, 0, 0);
            image.setImageDrawable(icon);
            image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            FlexboxLayout.LayoutParams layoutParams =
                    new FlexboxLayout.LayoutParams(
                            FlexboxLayout.LayoutParams.WRAP_CONTENT,
                            FlexboxLayout.LayoutParams.WRAP_CONTENT
                    );
            layoutParams.setFlexShrink(0.25f);
            layoutParams.setFlexGrow(0.f);
            layoutParams.setHeight(Utility.dpToPx(context, 24.f));
            image.setLayoutParams(layoutParams);
            container.addView(image);
        }
    }

    private Drawable getNotificationIcon(Context context, String packageName, int id) {
        Log.d(TAG, "getNotificationIcon for id = " + id);
        if (packageName == null || id == -1) return null;
        try {
            Context remotePackageContext = context.getApplicationContext().createPackageContext(packageName, 0);
            return ContextCompat.getDrawable(remotePackageContext, id);
        } catch (NameNotFoundException e) {
            return null;
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    public static void dumpIntent(Intent i){
        Bundle bundle = i.getExtras();
        if (bundle == null) return;
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            if (value != null) {
                Log.d(TAG, String.format("%s %s (%s)", key,
                            value.toString(), value.getClass().getName()));
            }
        }
    }
}
