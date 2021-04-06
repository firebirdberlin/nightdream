package com.firebirdberlin.nightdream.NotificationList;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;

public class Notification implements Parcelable {

    public static final Creator<Notification> CREATOR = new Creator<Notification>() {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public Notification createFromParcel(Parcel in) {
            return new Notification(in);
        }

        @Override
        public Notification[] newArray(int size) {
            return new Notification[size];
        }
    };
    public static String TAG = "Notification";
    private final Bitmap bigPicture;
    private final String time;
    private final long postTimestamp;
    private final String applicationName;
    private final String text;
    private final String textBig;
    private final String summaryText;
    private final RemoteViews remoteView;
    private View cardView;
    private View bigCardView;
    private Drawable drawableIcon;
    private final Bitmap bitmapLargeIcon;
    private final String title;
    private final String titleBig;
    private final String template;
    private Spanned notification_messages;
    private Spanned notification_textlines;
    private final PendingIntent pendingIntent;
    private final android.app.Notification.Action[] actions;
    private final String packageName;
    private final boolean isClearable;
    private int childId;
    private final Integer color;
    private final Integer notificationID;
    private final String notificationKey;
    private final String notificationTag;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected Notification(Parcel in) {
        bigPicture = in.readParcelable(Bitmap.class.getClassLoader());
        time = in.readString();
        postTimestamp = in.readLong();
        applicationName = in.readString();
        text = in.readString();
        textBig = in.readString();
        summaryText = in.readString();
        remoteView = in.readParcelable(RemoteViews.class.getClassLoader());
        bitmapLargeIcon = in.readParcelable(Bitmap.class.getClassLoader());
        title = in.readString();
        titleBig = in.readString();
        template = in.readString();
        pendingIntent = in.readParcelable(PendingIntent.class.getClassLoader());
        actions = in.createTypedArray(android.app.Notification.Action.CREATOR);
        packageName = in.readString();
        isClearable = in.readByte() != 0;
        childId = in.readInt();
        color = in.readInt();
        notificationID = in.readInt();
        notificationKey = in.readString();
        notificationTag = in.readString();
    }

    public Notification(Context context, Intent intent) {
        this.postTimestamp = intent.getLongExtra("postTimestamp", 0L);
        this.time = intent.getStringExtra("timestamp");
        this.template = intent.getStringExtra("template");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.notification_messages = Html.fromHtml(intent.getStringExtra("messages"), Html.FROM_HTML_MODE_LEGACY);
        } else {
            this.notification_messages = Html.fromHtml(intent.getStringExtra("messages"), null, null);
        }
        this.text = intent.getStringExtra("text");
        this.textBig = intent.getStringExtra("textBig");
        this.summaryText = intent.getStringExtra("summaryText");
        this.bigPicture = intent.getParcelableExtra("bigPicture");
        this.remoteView = intent.getParcelableExtra("contentView");
        this.applicationName = intent.getStringExtra("applicationName");
        this.title = intent.getStringExtra("title");
        this.titleBig = intent.getStringExtra("titleBig");
        this.actions = (android.app.Notification.Action[]) intent.getParcelableArrayExtra("actions");
        this.packageName = intent.getStringExtra("packageName");
        this.pendingIntent = intent.getParcelableExtra("contentIntent");
        this.isClearable = intent.getBooleanExtra("isClearable", true);
        this.color = intent.getIntExtra("color", 0);
        this.notificationID = intent.getIntExtra("id", 0);
        this.notificationKey = intent.getStringExtra("key");
        this.notificationTag = intent.getStringExtra("tag");

        //RemoteView to View
        try {
            FrameLayout container = new FrameLayout(context);
            RemoteViews notificationRemoteView = intent.getParcelableExtra("contentView");
            View view = notificationRemoteView.apply(context, container);
            container.addView(view);
            this.cardView = container;
        } catch (SecurityException | NullPointerException e) {
            this.cardView = null;
        }

        //BigRemoteView to View
        try {
            FrameLayout container = new FrameLayout(context);
            RemoteViews notificationRemoteBigView = intent.getParcelableExtra("bigView");
            View view = notificationRemoteBigView.apply(context, container);
            container.addView(view);
            this.bigCardView = container;
        } catch (SecurityException | NullPointerException e) {
            this.bigCardView = null;
        }

        //get drawable SmallIcon
        try {
            Context remotePackageContext = context.getApplicationContext().createPackageContext(intent.getStringExtra("packageName"), 0);
            this.drawableIcon = ContextCompat.getDrawable(remotePackageContext, intent.getIntExtra("iconId", 0));

        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }

        this.bitmapLargeIcon = intent.getParcelableExtra("largeIconBitmap");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.notification_textlines = Html.fromHtml(intent.getStringExtra("textLines"), Html.FROM_HTML_MODE_LEGACY);
        } else {
            this.notification_textlines = Html.fromHtml(intent.getStringExtra("textLines"), null, null);
        }

        childId = -1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(bigPicture, i);
        parcel.writeString(time);
        parcel.writeLong(postTimestamp);
        parcel.writeString(applicationName);
        parcel.writeString(text);
        parcel.writeString(textBig);
        parcel.writeString(summaryText);
        parcel.writeParcelable(remoteView, i);
        parcel.writeParcelable(bitmapLargeIcon, i);
        parcel.writeString(title);
        parcel.writeString(titleBig);
        parcel.writeString(template);
        parcel.writeParcelable(pendingIntent, i);
        parcel.writeTypedArray(actions, i);
        parcel.writeString(packageName);
        parcel.writeByte((byte) (isClearable ? 1 : 0));
        parcel.writeInt(childId);
        parcel.writeInt(color);
    }

    public String getText() {
        return text;
    }

    public String getTextBig() {
        return textBig;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public String getTemplate() {
        return template;
    }

    public Spanned getMessages() {
        return notification_messages;
    }

    public Spanned getTextLines() {
        return notification_textlines;
    }

    public Drawable getDrawableIcon() {
        return drawableIcon;
    }

    public Bitmap getBitmapLargeIcon() {
        return bitmapLargeIcon;
    }

    public String getTime() {
        return time;
    }

    public long getPostTimestamp() {
        return postTimestamp;
    }

    public String getTitle() {
        return title;
    }

    public String getTitleBig() {
        return titleBig;
    }

    public String getPostTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return dateFormat.format(postTimestamp);
    }

    public String getApplicationName() {
        return applicationName;
    }

    public Bitmap getBigPicture() {
        return bigPicture;
    }

    public View getCardView() {
        return cardView;
    }

    public View getBigCardView() {
        return bigCardView;
    }

    public PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    public android.app.Notification.Action[] getActions() {
        return actions;
    }

    public String getPackageName() {
        return packageName;
    }

    public Boolean isClearable() {
        return isClearable;
    }

    public void setChildId(int notification_child_id) {
        this.childId = notification_child_id;
    }

    public int getColor() {
        return color;
    }

    public int getNotificationID() {
        return notificationID;
    }

    public String getNotificationKey() {
        return notificationKey;
    }

    public String getNotificationTag() {
        return notificationTag;
    }
}

