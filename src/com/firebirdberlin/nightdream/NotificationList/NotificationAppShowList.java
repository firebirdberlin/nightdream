package com.firebirdberlin.nightdream.NotificationList;


import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationAppShowList {

    private List<NotificationApp> notificationappshowlist = new ArrayList<NotificationApp>();
    private SharedPreferences sharedPreferences;

    public NotificationAppShowList(List<NotificationApp> notificationappshowlist, Context context) {
        this.notificationappshowlist.addAll(notificationappshowlist);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public List<NotificationApp> get_notificationappshowlist(){return this.notificationappshowlist;}

    public void clear_notificationappshowlist(){notificationappshowlist.clear(); }


    public void change_notificationsapphowlist(List<NotificationApp> notificationapplist) {

        clear_notificationappshowlist();

        for (NotificationApp notifyapp : notificationapplist) {
            notificationappshowlist.add(notifyapp);
        }

        //sorting time descending order
        Collections.sort(notificationappshowlist, new Comparator<NotificationApp>() {
            @Override
            public int compare(NotificationApp obj1, NotificationApp obj2) {
                return obj2.get_notificationapp_posttime().compareToIgnoreCase(obj1.get_notificationapp_posttime());
            }
        });
    }
}

