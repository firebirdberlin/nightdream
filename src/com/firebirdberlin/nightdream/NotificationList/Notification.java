package com.firebirdberlin.nightdream.NotificationList;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;

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
    private final Bitmap bitmapLargeIcon;
    private final String title;
    private final String titleBig;
    private final String template;
    private final PendingIntent pendingIntent;
    private final android.app.Notification.Action[] actions;
    private final String packageName;
    private final boolean isClearable;
    private final Integer color;
    private final Integer notificationID;
    private final String notificationKey;
    private final String notificationTag;
    private View cardView;
    private View bigCardView;
    private Bitmap smallIconBitmap;
    private Spanned notification_messages;
    private Spanned notification_textlines;
    private int childId;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected Notification(Parcel in) {
        bigPicture = (Bitmap) getParcelable(in, Bitmap.class.getClassLoader());
        time = in.readString();
        postTimestamp = in.readLong();
        applicationName = in.readString();
        text = in.readString();
        textBig = in.readString();
        summaryText = in.readString();
        bitmapLargeIcon = (Bitmap) getParcelable(in, Bitmap.class.getClassLoader());
        title = in.readString();
        titleBig = in.readString();
        template = in.readString();
        pendingIntent = (PendingIntent) getParcelable(in, PendingIntent.class.getClassLoader());
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

        this.cardView = getCard(context, intent, "contentView");
        this.bigCardView = getCard(context, intent, "bigContentView");

        this.smallIconBitmap = intent.getParcelableExtra("smallIconBitmap");
        this.bitmapLargeIcon = intent.getParcelableExtra("largeIconBitmap");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.notification_textlines = Html.fromHtml(intent.getStringExtra("textLines"), Html.FROM_HTML_MODE_LEGACY);
        } else {
            this.notification_textlines = Html.fromHtml(intent.getStringExtra("textLines"), null, null);
        }

        childId = -1;
    }

    View getCard(Context context, Intent intent, String name) {
        try {
            RemoteViews remoteView = intent.getParcelableExtra(name);
            if (remoteView != null) {
                FrameLayout container = new FrameLayout(context);
                container.addView(remoteView.apply(context, container));
                return container;
            }
        } catch (SecurityException | RemoteViews.ActionException ignored) {}
        return null;
    }

    Parcelable getParcelable(Parcel in, ClassLoader classLoader) {
        try {
            return in.readParcelable(classLoader);
        } catch (ClassCastException ignored) {
            return null;
        }
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

    public Bitmap getSmallIconBitmap() {
        return smallIconBitmap;
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

