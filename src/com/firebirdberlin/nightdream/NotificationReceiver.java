package com.firebirdberlin.nightdream;

import android.app.Notification;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.google.android.flexbox.FlexboxLayout;

import java.util.Objects;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    public static final String PREFS_KEY = "NightDream preferences";
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

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            handleOnReceive(context, intent);
        } catch (NullPointerException e) {
            Log.e(TAG, "Unknown error in onReceive (NotificationReceiver)");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void handleOnReceive(final Context context, Intent intent) {

        if (intent == null || context == null) return;
        if (Utility.isDebuggable(context)){
            Log.d(TAG, "Broadcast received.");
            dumpIntent(intent);
        }

        FlexboxLayout notificationBar = contentView.findViewById(R.id.notificationbar);
        FlexboxLayout notificationStatusBar = contentView.findViewById(R.id.notificationstatusbar);
        FlexboxLayout container =
                (Settings.useNotificationStatusBar(context)) ? notificationStatusBar : notificationBar;
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
            layoutParams.setMaxWidth(Utility.dpToPx(context, 24.f));
            layoutParams.setMaxHeight(Utility.dpToPx(context, 24.f));
            image.setLayoutParams(layoutParams);
            container.addView(image);

            //MediaStyle Control

            FlexboxLayout mediastylecontrolcontainer = contentView.findViewById(R.id.notification_mediacontrol_bar);

            SharedPreferences preferences = context.getSharedPreferences(PREFS_KEY, 0);
            boolean showmediastyle = preferences.getBoolean("showMediaStyle", false);

            if (Objects.requireNonNull(intent.getStringExtra("template")).contains("MediaStyle") && showmediastyle) {
                Log.w(TAG, "Show MediaStyle notification");

                mediastylecontrolcontainer.setVisibility(View.VISIBLE);
                mediastylecontrolcontainer.removeAllViews();

                LayoutInflater inflater = (LayoutInflater)
                        context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );

                View mediastylecontrol = inflater.inflate(R.layout.notification_mediacontrol, null);
                mediastylecontrol.findViewById(R.id.notify_largeicon).setVisibility(View.GONE);
                mediastylecontrol.findViewById(R.id.notify_largepicture).setVisibility(View.GONE);

                ImageView notificationMessagebitmap = (ImageView) mediastylecontrol.findViewById(R.id.notify_smallicon);
                Drawable notificationMessagesmallicon = getNotificationIcon(context, packageName, iconId);
                assert notificationMessagesmallicon != null;
                notificationMessagesmallicon.setColorFilter(Color.parseColor("#000000"), PorterDuff.Mode.SRC_ATOP);
                notificationMessagebitmap.setImageDrawable(getNotificationIcon(context, packageName, iconId));

                TextView appname = mediastylecontrol.findViewById(R.id.notify_appname);
                appname.setText((String) intent.getStringExtra("applicationname"));

                TextView timestamp = mediastylecontrol.findViewById(R.id.notify_timestamp);
                timestamp.setText((String) intent.getStringExtra("posttimestamp"));

                TextView title = mediastylecontrol.findViewById(R.id.notify_title);
                title.setText((String) intent.getStringExtra("title"));

                TextView ntext = mediastylecontrol.findViewById(R.id.notify_text);
                ntext.setText((String) intent.getStringExtra("text"));

                mediastylecontrol.findViewById(R.id.notification_control).setVisibility(View.VISIBLE);
                mediastylecontrol.findViewById(R.id.notify_background).setBackground(new BitmapDrawable(context.getResources(),(Bitmap) intent.getParcelableExtra("largeiconbitmap")));
                mediastylecontrol.findViewById(R.id.notify_actiontext1).setVisibility(View.GONE);
                mediastylecontrol.findViewById(R.id.notify_actiontext2).setVisibility(View.GONE);
                mediastylecontrol.findViewById(R.id.notify_actiontext3).setVisibility(View.GONE);

                ImageView[] notificationActionImages = new ImageView[5];
                notificationActionImages[0] = (ImageView) mediastylecontrol.findViewById(R.id.notify_actionview1);
                notificationActionImages[1] = (ImageView) mediastylecontrol.findViewById(R.id.notify_actionview2);
                notificationActionImages[2] = (ImageView) mediastylecontrol.findViewById(R.id.notify_actionview3);
                notificationActionImages[3] = (ImageView) mediastylecontrol.findViewById(R.id.notify_actionview4);
                notificationActionImages[4] = (ImageView) mediastylecontrol.findViewById(R.id.notify_actionview5);

                for( ImageView ActionImage: notificationActionImages )
                {
                    ActionImage.setVisibility(View.GONE);
                }

                int positionAction = 0;

                try {
                    for (final Notification.Action actionm : (Notification.Action[]) intent.getParcelableArrayExtra("actions")) {
                        try {
                            Context remotePackageContext = context.getApplicationContext().createPackageContext((String) (String) intent.getStringExtra("packageName"), 0);
                            Drawable notification_drawableicon = ContextCompat.getDrawable(remotePackageContext, (int) actionm.getIcon().getResId());
                            notificationActionImages[positionAction].setImageDrawable(notification_drawableicon);
                            notificationActionImages[positionAction].setVisibility(View.VISIBLE);
                            notificationActionImages[positionAction].setOnClickListener(new View.OnClickListener() {
                                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                                public void onClick(View v) {
                                    try {
                                        actionm.actionIntent.send();
                                    } catch (Exception ex) {
                                        Log.e(TAG, "MediaStyle - Notification set actionIntent");
                                    }
                                }
                            });
                            positionAction++;
                        } catch (Exception exMediaStyle) {
                            Log.w(TAG, "MediaStyle - Notification set Icon");
                        }
                    }
                }catch (Exception ex){
                    Log.e(TAG, "MediaStyle - Notification actions");
                }
                mediastylecontrolcontainer.addView(mediastylecontrol);
            }
        }
    }

    private void removeViewsFrom(FlexboxLayout layout) {
        if (layout != null) layout.removeAllViews();
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
