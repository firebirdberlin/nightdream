package com.firebirdberlin.nightdream.ui;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.databinding.NotificationMediacontrolBinding;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

public class MediaControlLayout extends ViewModel {
    private static String TAG = "MediaControlLayout";
    private int color = 0;
    private Drawable smallAppIcon;

    private final MutableLiveData<Drawable> smallIcon = new MediatorLiveData<>();
    private final MutableLiveData<Drawable> largeIcon = new MediatorLiveData<>();
    private final MutableLiveData<Integer> textColor = new MediatorLiveData<>();
    private final MutableLiveData<String> appName = new MediatorLiveData<>();
    private final MutableLiveData<String> timeStamp = new MediatorLiveData<>();
    private final MutableLiveData<String> title = new MediatorLiveData<>();
    private final MutableLiveData<String> text = new MediatorLiveData<>();
    private final MutableLiveData<ArrayList<Drawable>> actionImages = new MediatorLiveData<>();
    private final MutableLiveData<ArrayList<View.OnClickListener>> actionIntent = new MediatorLiveData<>();

    private NotificationMediacontrolBinding mediacontrolBinding;

    public MediaControlLayout(ConstraintLayout mediaStyleContainer) {
        LayoutInflater inflater = LayoutInflater.from(mediaStyleContainer.getContext());
        mediacontrolBinding = NotificationMediacontrolBinding.inflate(inflater, mediaStyleContainer, false);
        mediacontrolBinding.setModel(this);
    }

    public View getView() {
        return mediacontrolBinding.getRoot();
    }

    public void setColor(int color) {
        this.color = color;
        setColor();
    }

    private void setColor() {
        this.textColor.setValue(this.color);

        if (this.smallIcon.getValue() != null) {
            this.smallIcon.getValue().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }

        if (this.actionImages.getValue() != null) {
            for (int i = 0; i < this.actionImages.getValue().size(); i++) {
                this.actionImages.getValue().get(i).setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            }
        }
    }

    public LiveData<Drawable> getSmallIcon() {
        return smallIcon;
    }

    public LiveData<Drawable> getLargeIcon() {
        return largeIcon;
    }

    public LiveData<Integer> getTextColor() {
        return textColor;
    }

    public LiveData<String> getAppName() {
        return appName;
    }

    public LiveData<String> getTimeStamp() {
        return timeStamp;
    }

    public LiveData<String> getTitle() {
        return title;
    }

    public LiveData<String> getText() {
        return text;
    }

    public LiveData<ArrayList<Drawable>> getActionImages() {
        return actionImages;
    }

    public LiveData<ArrayList<View.OnClickListener>> getActionIntent() {
        return actionIntent;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void setupFromNotificationIntent(Context context, Intent intent, Drawable smallIcon) {
        Log.d(TAG, "setupFromNotificationIntent");

        String template = intent.getStringExtra("template");
        if (template != null && !template.contains("MediaStyle")) {
            return;
        }

        assert smallIcon != null;
        smallIcon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        Bitmap coverBitmap = intent.getParcelableExtra("largeIconBitmap");

        ArrayList<Drawable> notificationActionImages = new ArrayList<>();
        ArrayList<View.OnClickListener> notificationActionClick = new ArrayList<>();

        Context remotePackageContext = null;
        try {
            remotePackageContext =
                    context.getApplicationContext().createPackageContext(
                            intent.getStringExtra("packageName"), 0
                    );
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        Notification.Action[] actions =
                (Notification.Action[]) intent.getParcelableArrayExtra("actions");
        if (actions != null) {
            for (final Notification.Action action : actions) {
                Drawable notificationDrawableIcon = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Icon icon = action.getIcon();
                    if (icon != null) {
                        notificationDrawableIcon = icon.loadDrawable(remotePackageContext);
                        notificationDrawableIcon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    }
                } else {
                    int iconResId = action.icon;
                    notificationDrawableIcon = ContextCompat.getDrawable(remotePackageContext, iconResId);
                    notificationDrawableIcon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                }
                notificationActionImages.add(notificationDrawableIcon);

                notificationActionClick.add(
                    new View.OnClickListener() {
                        public void onClick(View v) {
                            try {
                                action.actionIntent.send();
                            } catch (PendingIntent.CanceledException ex) {
                                Log.e(TAG, "MediaStyle - Notification set actionIntent");
                            }
                        }
                    }
                );
            }
        }

        this.smallIcon.setValue(smallIcon);
        this.appName.setValue(intent.getStringExtra("applicationName"));
        this.timeStamp.setValue(intent.getStringExtra("postTimestamp"));
        this.title.setValue(intent.getStringExtra("title"));
        this.text.setValue(intent.getStringExtra("text"));
        this.largeIcon.setValue(new BitmapDrawable(context.getResources(), coverBitmap));
        this.actionImages.setValue(notificationActionImages);
        this.actionIntent.setValue(notificationActionClick);
        setColor();
    }
}
