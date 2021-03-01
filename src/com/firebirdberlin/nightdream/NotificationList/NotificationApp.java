package com.firebirdberlin.nightdream.NotificationList;


import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;
import java.util.Comparator;

public class NotificationApp implements Parcelable {

    public static final Creator<NotificationApp> CREATOR = new Creator<NotificationApp>() {
        @Override
        public NotificationApp createFromParcel(Parcel in) {
            return new NotificationApp(in);
        }

        @Override
        public NotificationApp[] newArray(int size) {
            return new NotificationApp[size];
        }
    };
    public static Comparator<NotificationApp> comparator = new Comparator<NotificationApp>() {
        @Override
        public int compare(NotificationApp obj1, NotificationApp obj2) {
            long t1 = obj1.getPostTimestamp();
            long t2 = obj2.getPostTimestamp();
            if (t1 == t2) {
                return 0;
            } else if (t1 > t2) {
                return -1;
            }
            return 1;
        }
    };
    private final Bitmap picture;
    private final String name;
    private final String packageName;
    private int iconId;
    private long postTimestamp;

    protected NotificationApp(Parcel parcel) {
        picture = parcel.readParcelable(Bitmap.class.getClassLoader());
        postTimestamp = parcel.readLong();
        name = parcel.readString();
        packageName = parcel.readString();
    }

    public NotificationApp(Intent intent) {
        this.iconId = intent.getIntExtra("iconId", -1);
        this.name = intent.getStringExtra("applicationName");
        this.packageName = intent.getStringExtra("packageName");
        this.picture = intent.getParcelableExtra("bitmap");
        this.postTimestamp = intent.getLongExtra("postTimestamp", 0L);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(picture, i);
        parcel.writeLong(postTimestamp);
        parcel.writeString(name);
        parcel.writeString(packageName);
    }

    public Bitmap getPicture() {
        return picture;
    }

    public String getName() {
        if (name == null) return "";
        return name;
    }

    public long getPostTimestamp() {
        return postTimestamp;
    }

    public void setPostTimestamp(long postTimestamp) {
        this.postTimestamp = postTimestamp;
    }

    public String getPostTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return dateFormat.format(postTimestamp);
    }

    public String getPackageName() {
        return packageName;
    }

    public int getIconId() {
        return iconId;
    }
}
