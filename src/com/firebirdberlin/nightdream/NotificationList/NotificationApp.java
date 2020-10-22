package com.firebirdberlin.nightdream.NotificationList;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class NotificationApp implements Parcelable {

    protected NotificationApp(Parcel in) {
        notificationapp_picture = in.readParcelable(Bitmap.class.getClassLoader());
        notificationapp_time = in.readString();
        notificationapp_name = in.readString();
        notificationapp_package = in.readString();
    }

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(notificationapp_picture, i);
        parcel.writeString(notificationapp_time);
        parcel.writeString(notificationapp_name);
        parcel.writeString(notificationapp_package);
    }

    private Bitmap notificationapp_picture;
    private String notificationapp_time;
    private String notificationapp_timestamp;
    private String notificationapp_name;
    private String notificationapp_package;

    public NotificationApp(Context context, Intent intent) {

        this.notificationapp_picture = intent.getParcelableExtra("bitmap");
        this.notificationapp_time =  intent.getStringExtra("timestamp");
        this.notificationapp_name =  intent.getStringExtra("name");
        this.notificationapp_timestamp =  intent.getStringExtra("posttimestamp");
        this.notificationapp_package = intent.getStringExtra("package");
    }

    public Bitmap get_notificationapp_picture() {
        return notificationapp_picture;
    }

    public String get_notificationapp_time() {
        return notificationapp_time;
    }

    public String get_notificationapp_name() {return notificationapp_name;}

    public String get_notificationapp_posttime() {return notificationapp_timestamp;}

    public String get_notificationapp_package() {
        return notificationapp_package;
    }

}
