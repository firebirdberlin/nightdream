package com.firebirdberlin.nightdream.NotificationList;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class notificationshowlist {
    private List<Notification> notificationshowlist = new ArrayList<Notification>();

    private SharedPreferences sharedPreferences;

    public notificationshowlist(List<Notification> notificationshowlist, Context context) {
        this.notificationshowlist.addAll(notificationshowlist);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public List<Notification> get_notificationshowlist(){return this.notificationshowlist;}

    public void clear_notificationshowlist(){notificationshowlist.clear(); }

    private boolean check_clearable(Notification notify){
        if (sharedPreferences.getBoolean("shownotclearable", false)) {
            return true;
        }
        else {
            return !notify.get_notification_clearable() == sharedPreferences.getBoolean("shownotclearable", false);
        }
    }

    private boolean check_packagename(Notification notify, String packagename){
        if (packagename.equals("")){
            return true;
        }

        return notify.get_notification_package().equals(packagename);
    }

    public void change_notificationshowlist(List<Notification> notificationlist, String packagename) {

        clear_notificationshowlist();

        for (int index = 0; index < notificationlist.size(); index++) {
            if (check_clearable(notificationlist.get(index)) && check_packagename(notificationlist.get(index), packagename)) {
                notificationlist.get(index).set_notification_child_id(index);
                notificationshowlist.add(notificationlist.get(index));
            }
        }

        //sorting time descending order
        Collections.sort(notificationshowlist, new Comparator<Notification>() {
            @Override
            public int compare(Notification obj1, Notification obj2) {
                return obj2.get_notification_posttimestamp().compareToIgnoreCase(obj1.get_notification_posttimestamp());
            }
        });
    }
}
