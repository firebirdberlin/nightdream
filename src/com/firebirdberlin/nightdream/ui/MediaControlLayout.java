package com.firebirdberlin.nightdream.ui;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.firebirdberlin.nightdream.R;

public class MediaControlLayout extends LinearLayout {
    private static String TAG = "MediaControlLayout";
    public MediaControlLayout(Context context) {
        super(context);
    }

    public MediaControlLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void setupFromNotificationIntent(Context context, Intent intent, Drawable smallIcon) {
        String template = intent.getStringExtra("template");
        if (template != null && !template.contains("MediaStyle")) {
            return;
        }
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View mediaStyleControl = inflater.inflate(R.layout.notification_mediacontrol, null);
        ImageView largeIconImageView = mediaStyleControl.findViewById(R.id.notify_largeicon);

        ImageView notificationMessageBitmap = mediaStyleControl.findViewById(R.id.notify_smallicon);
        assert smallIcon != null;
        smallIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        notificationMessageBitmap.setImageDrawable(smallIcon);

        TextView appName = mediaStyleControl.findViewById(R.id.notify_appname);
        appName.setText(intent.getStringExtra("applicationName"));

        TextView timestamp = mediaStyleControl.findViewById(R.id.notify_timestamp);
        timestamp.setText(intent.getStringExtra("postTimestamp"));

        TextView title = mediaStyleControl.findViewById(R.id.notify_title);
        title.setText(intent.getStringExtra("title"));

        TextView ntext = mediaStyleControl.findViewById(R.id.notify_text);
        ntext.setText(intent.getStringExtra("text"));

        Bitmap coverBitmap = intent.getParcelableExtra("largeIconBitmap");
        largeIconImageView.setImageBitmap(coverBitmap);
        mediaStyleControl.findViewById(R.id.notification_control).setVisibility(View.VISIBLE);

        ImageView[] notificationActionImages = new ImageView[5];
        notificationActionImages[0] = mediaStyleControl.findViewById(R.id.notify_actionview1);
        notificationActionImages[1] = mediaStyleControl.findViewById(R.id.notify_actionview2);
        notificationActionImages[2] = mediaStyleControl.findViewById(R.id.notify_actionview3);
        notificationActionImages[3] = mediaStyleControl.findViewById(R.id.notify_actionview4);
        notificationActionImages[4] = mediaStyleControl.findViewById(R.id.notify_actionview5);

        for (ImageView ActionImage : notificationActionImages) {
            ActionImage.setVisibility(View.GONE);
        }

        int positionAction = 0;
        try {
            for (final Notification.Action action : (Notification.Action[]) intent.getParcelableArrayExtra("actions")) {
                try {
                    Context remotePackageContext = context.getApplicationContext().createPackageContext(
                            intent.getStringExtra("packageName"), 0
                    );

                    Drawable notificationDrawableIcon = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        Icon icon = action.getIcon();
                        if (icon != null) {
                            notificationDrawableIcon = icon.loadDrawable(remotePackageContext);
                        }
                    } else {
                        int iconResId = action.icon;
                        notificationDrawableIcon = ContextCompat.getDrawable(remotePackageContext, iconResId);
                    }
                    notificationActionImages[positionAction].setImageDrawable(notificationDrawableIcon);
                    notificationActionImages[positionAction].setVisibility(View.VISIBLE);
                    notificationActionImages[positionAction].setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            try {
                                action.actionIntent.send();
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
        } catch (Exception ex) {
            Log.e(TAG, "MediaStyle - Notification actions");
        }
        removeAllViews();
        addView(mediaStyleControl);
        setVisibility(View.VISIBLE);
    }
}
