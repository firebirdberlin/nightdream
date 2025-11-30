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
    private final Bitmap smallNotificationIcon;
    private final String name;
    private final String packageName;
    private int iconId;
    private long postTimestamp;

    protected NotificationApp(Parcel parcel) {
        this.picture = parcel.readParcelable(Bitmap.class.getClassLoader());
        this.postTimestamp = parcel.readLong();
        this.name = parcel.readString();
        this.packageName = parcel.readString();
        this.smallNotificationIcon = parcel.readParcelable(Bitmap.class.getClassLoader());
    }

    public NotificationApp(Intent intent) {
        this.iconId = intent.getIntExtra("iconId", -1);
        this.name = intent.getStringExtra("applicationName");
        this.packageName = intent.getStringExtra("packageName");
        this.picture = intent.getParcelableExtra("bitmap");
        this.smallNotificationIcon = intent.getParcelableExtra("smallIconBitmap");
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
        parcel.writeParcelable(smallNotificationIcon, i);
    }

    public Bitmap getPicture() {
        return picture;
    }

    public Bitmap getSmallNotificationIcon() {
        return smallNotificationIcon;
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
