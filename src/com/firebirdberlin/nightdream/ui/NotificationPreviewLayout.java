package com.firebirdberlin.nightdream.ui;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.firebirdberlin.nightdream.R;

public class NotificationPreviewLayout extends LinearLayout {
    private static String TAG = "NotificationPreviewLayout";
    private View notificationPreview;
    final private Handler handler = new Handler();

    public NotificationPreviewLayout(Context context) {
        super(context);
    }

    public NotificationPreviewLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    private Runnable fadePreview = new Runnable() {
        @Override
        public void run() {
                AlphaAnimation alpha;
                alpha = new AlphaAnimation(1.0f, 0.0f);
                alpha.setDuration(2000);
                alpha.setFillAfter(true);

                AnimationSet animationSet = new AnimationSet(true);

                animationSet.addAnimation(alpha);

                animationSet.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        notificationPreview.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                notificationPreview.startAnimation(animationSet);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void setupFromNotificationIntent(Context context, final Intent intent, Drawable smallIcon) {
        Log.i(TAG, "NotificationPreview");
        String template = intent.getStringExtra("template");
        if (template != null && template.contains("MediaStyle")) {
            return;
        }

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        notificationPreview = inflater.inflate(R.layout.notification_preview, null);
        ImageView largeIconImageView = notificationPreview.findViewById(R.id.notify_largeicon);

        ImageView notificationMessageBitmap = notificationPreview.findViewById(R.id.notify_smallicon);
        assert smallIcon != null;
        smallIcon.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
        notificationMessageBitmap.setImageDrawable(smallIcon);

        TextView appName = notificationPreview.findViewById(R.id.notify_appname);
        appName.setText(intent.getStringExtra("applicationName"));

        TextView timestamp = notificationPreview.findViewById(R.id.notify_timestamp);
        timestamp.setText(intent.getStringExtra("postTimestamp"));

        TextView title = notificationPreview.findViewById(R.id.notify_title);
        title.setText(intent.getStringExtra("title"));

        TextView ntext = notificationPreview.findViewById(R.id.notify_text);
        ntext.setText(intent.getStringExtra("text"));

        Bitmap coverBitmap = intent.getParcelableExtra("largeIconBitmap");
        if (coverBitmap == null)
        {
            largeIconImageView.setVisibility(View.GONE);
        }
        else {
            largeIconImageView.setImageBitmap(coverBitmap);
            notificationPreview.findViewById(R.id.notification_control).setVisibility(View.VISIBLE);
        }

        OnClickListener onNotificationPreviewClickListener = new OnClickListener() {
            public void onClick(View v) {
                PendingIntent contentIntent = (PendingIntent) intent.getParcelableExtra("contentintent");
                if (contentIntent != null) {
                    try {
                        contentIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        try {
                            throw new IntentSender.SendIntentException(e);
                        } catch (IntentSender.SendIntentException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        };

        notificationPreview.setOnClickListener(onNotificationPreviewClickListener);

        removeAllViews();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
             GradientDrawable shape = new GradientDrawable();
             shape.setCornerRadius(30);
             shape.setColor(Color.parseColor("#FFFFFFFF"));
             notificationPreview.findViewById(R.id.notify_linear_layout).setBackground(shape);
        } else {
           notificationPreview.findViewById(R.id.notify_linear_layout).setBackgroundColor(Color.parseColor("#FFFFFFFF"));
        }

        addView(notificationPreview);
        setVisibility(View.VISIBLE);
        handler.postDelayed(fadePreview,10000);
    }
}
