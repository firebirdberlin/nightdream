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
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

public class Notification implements Parcelable {

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected Notification(Parcel in) {
        notification_picture = in.readParcelable(Bitmap.class.getClassLoader());
        notification_bigpicture = in.readParcelable(Bitmap.class.getClassLoader());
        notification_time = in.readString();
        notification_posttimestamp = in.readString();
        notification_posttime = in.readString();
        notification_applicationname = in.readString();
        notification_text = in.readString();
        notification_textbig = in.readString();
        notification_summarytext = in.readString();
        notification_remoteview = in.readParcelable(RemoteViews.class.getClassLoader());
        notification_bitmaplargeicon = in.readParcelable(Bitmap.class.getClassLoader());
        notification_title = in.readString();
        notification_titlebig = in.readString();
        notification_template = in.readString();
        notification_contentintent = in.readParcelable(PendingIntent.class.getClassLoader());
        notification_action = in.createTypedArray(android.app.Notification.Action.CREATOR);
        notification_package = in.readString();
        notification_clearable = in.readByte() != 0;
        notification_child_id = in.readInt();
    }

    public static final Creator<Notification> CREATOR = new Creator<Notification>() {
        @Override
        public Notification createFromParcel(Parcel in) {
            return new Notification(in);
        }

        @Override
        public Notification[] newArray(int size) {
            return new Notification[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(notification_picture, i);
        parcel.writeParcelable(notification_bigpicture, i);
        parcel.writeString(notification_time);
        parcel.writeString(notification_posttimestamp);
        parcel.writeString(notification_posttime);
        parcel.writeString(notification_applicationname);
        parcel.writeString(notification_text);
        parcel.writeString(notification_textbig);
        parcel.writeString(notification_summarytext);
        parcel.writeParcelable(notification_remoteview, i);
        parcel.writeParcelable(notification_bitmaplargeicon, i);
        parcel.writeString(notification_title);
        parcel.writeString(notification_titlebig);
        parcel.writeString(notification_template);
        parcel.writeParcelable(notification_contentintent, i);
        parcel.writeTypedArray(notification_action, i);
        parcel.writeString(notification_package);
        parcel.writeByte((byte) (notification_clearable ? 1 : 0));
        parcel.writeInt(notification_child_id);
    }

    private Bitmap notification_picture;
    private Bitmap notification_bigpicture;
    private String notification_time;
    private String notification_posttimestamp;
    private String notification_posttime;
    private String notification_applicationname;
    private Spanned notification_htmltext;
    private String notification_text;
    private String notification_textbig;
    private String notification_summarytext;
    private RemoteViews notification_remoteview;
    private View notification_cardview;
    private View notification_bigcardview;
    private Drawable notification_drawableicon;
    private Bitmap notification_bitmaplargeicon;
    private String notification_title;
    private String notification_titlebig;
    private String notification_template;
    private Spanned notification_messages;
    private Spanned notification_textlines;
    private PendingIntent notification_contentintent;
    private android.app.Notification.Action[] notification_action;
    private String notification_package;
    private boolean notification_clearable;
    private int notification_child_id;

    public Notification(Context context, Intent intent) {

        this.notification_posttimestamp=  intent.getStringExtra("posttimestamp");
        this.notification_time=  intent.getStringExtra("timestamp");
        this.notification_posttime=  intent.getStringExtra("posttime");
        this.notification_template=  intent.getStringExtra("template");
        this.notification_htmltext= Html.fromHtml((String) intent.getStringExtra("htmltext"),Html.FROM_HTML_MODE_LEGACY);
        this.notification_messages= Html.fromHtml((String) intent.getStringExtra("messages"),Html.FROM_HTML_MODE_LEGACY);
        this.notification_text= (String) intent.getStringExtra("text");
        this.notification_textbig= (String) intent.getStringExtra("textbig");
        this.notification_summarytext= (String) intent.getStringExtra("summarytext");
        this.notification_picture= (Bitmap) intent.getParcelableExtra("bitmap");
        this.notification_bigpicture= (Bitmap) intent.getParcelableExtra("bigpicture");
        this.notification_remoteview = (RemoteViews) intent.getParcelableExtra("view");
        this.notification_applicationname = (String) intent.getStringExtra("applicationname");
        this.notification_title = (String) intent.getStringExtra("title");
        this.notification_titlebig = (String) intent.getStringExtra("titlebig");
        this.notification_action = (android.app.Notification.Action[]) intent.getParcelableArrayExtra("action");
        this.notification_package = (String) intent.getStringExtra("package");
        this.notification_contentintent= (PendingIntent) intent.getParcelableExtra("contentintent");
        this.notification_clearable = (Boolean) intent.getBooleanExtra("clearable", true);

        //RemoteView to View
        FrameLayout CONTAINER = new FrameLayout(context);
        try {
            RemoteViews notificationRemoteView = (RemoteViews) intent.getParcelableExtra("view");
            View view = notificationRemoteView.apply(context, CONTAINER);
            CONTAINER.addView(view);
            this.notification_cardview = CONTAINER;
        } catch (Exception ex) {
            /*Button yourButton = new Button(context);
            //do stuff like add text and listeners.

            CONTAINER.addView(yourButton);
            this.notification_cardview = CONTAINER;*/
        }

        //BigRemoteView to View
        CONTAINER = new FrameLayout(context);
        try {
            RemoteViews notificationRemoteView = (RemoteViews) intent.getParcelableExtra("bigview");
            View view = notificationRemoteView.apply(context, CONTAINER);
            CONTAINER.addView(view);
            this.notification_bigcardview = CONTAINER;
        } catch (Exception ex) {

        }

        //get drawable SmallIcon
        try{
            Context remotePackageContext = context.getApplicationContext().createPackageContext((String) intent.getStringExtra("package"), 0);
            this.notification_drawableicon = ContextCompat.getDrawable(remotePackageContext, (int) intent.getIntExtra("iconresid",0));

        }catch (Exception ex){

        }

        this.notification_bitmaplargeicon = (Bitmap) intent.getParcelableExtra("largeiconbitmap");
        this.notification_textlines = Html.fromHtml((String) intent.getStringExtra("textlines"),Html.FROM_HTML_MODE_LEGACY);

        notification_child_id = -1;
    }

    public String get_notification_text() {
        return notification_text;
    }

    public String get_notification_textbig() {return notification_textbig;}

    public String get_notification_summarytext() {
        return notification_summarytext;
    }

    public String get_notification_template() {
        return notification_template;
    }

    public Spanned get_notification_htmltext() {
        return notification_htmltext;
    }

    public Spanned get_notification_messages() {
        return notification_messages;
    }

    public Spanned get_notification_textlines() {
        return notification_textlines;
    }

    public Drawable get_notification_drawableicon() {return notification_drawableicon;}

    public Bitmap get_notification_bitmaplargeicon() {return notification_bitmaplargeicon;}

    public String get_notification_time() {
        return notification_time;
    }

    public String get_notification_posttimestamp() {
        return notification_posttimestamp;
    }

    public String get_notification_title() {
        return notification_title;
    }

    public String get_notification_titlebig() {
        return notification_titlebig;
    }

    public String get_notification_posttime() {return notification_posttime;}

    public String get_notification_applicationname() {
        return notification_applicationname;
    }

    public Bitmap get_notification_picture() {
        return notification_picture;
    }

    public Bitmap get_notification_bigpicture() {
        return notification_bigpicture;
    }

    public RemoteViews get_notification_remoteview () {return notification_remoteview;}

    public View get_notification_view () {return notification_cardview;}

    public View get_notification_bigview () {return notification_bigcardview;}

    public PendingIntent get_notification_contentintent () {return notification_contentintent;}

    public android.app.Notification.Action[] get_notification_action() {return notification_action;}

    public String get_notification_package() {
        return notification_package;
    }

    public Boolean get_notification_clearable() {
        return notification_clearable;
    }

    public int get_notification_child_id(){return notification_child_id;}

    public void set_notification_child_id(int notification_child_id){this.notification_child_id = notification_child_id;}

}

