package com.firebirdberlin.nightdream.viewmodels;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.firebirdberlin.nightdream.NotificationList.NotificationApp;

import java.util.List;

public class NotificationViewModel extends ViewModel {

    private static final MutableLiveData<List<NotificationApp>> notificationAppsLiveData = new MutableLiveData<>();
    private static final MutableLiveData<Integer> resourceIdLiveData = new MutableLiveData<>();
    private static final MutableLiveData<Boolean> showNotificationLiveData = new MutableLiveData<>();
    private static final MutableLiveData<Integer> textColorLiveData = new MutableLiveData<>();

    public static void setNotificationApp(List<NotificationApp> notificationApps) {
        notificationAppsLiveData.setValue(notificationApps);
    }

    public static void setNotificationContainerResourceId(int resourceId) {
        resourceIdLiveData.setValue(resourceId);
    }

    public static void setShowNotification(boolean showNotification) {
        showNotificationLiveData.setValue(showNotification);
    }

    public static void setTextColor(int textColor) {
        textColorLiveData.setValue(textColor);
    }

    private MutableLiveData<List<NotificationApp>> getDataNotificationApps() {
        return notificationAppsLiveData;
    }

    private MutableLiveData<Integer> getDataResourceId() {
        return resourceIdLiveData;
    }

    private MutableLiveData<Boolean> getDataShowNotification() {
        return showNotificationLiveData;
    }

    private MutableLiveData<Integer> getDataTextColor() {
        return textColorLiveData;
    }

    public static void observeNotificationApp(Context context, @NonNull Observer<List<NotificationApp>> observer) {
        NotificationViewModel model = new ViewModelProvider((ViewModelStoreOwner) context).get(NotificationViewModel.class);
        model.getDataNotificationApps().observe((LifecycleOwner) context, observer);
    }

    public static void observeResourceId(Context context, @NonNull Observer<Integer> observer) {
        NotificationViewModel model = new ViewModelProvider((ViewModelStoreOwner) context).get(NotificationViewModel.class);
        model.getDataResourceId().observe((LifecycleOwner) context, observer);
    }

    public static void observeShowNotification(Context context, @NonNull Observer<Boolean> observer) {
        NotificationViewModel model = new ViewModelProvider((ViewModelStoreOwner) context).get(NotificationViewModel.class);
        model.getDataShowNotification().observe((LifecycleOwner) context, observer);
    }

    public static void observeTextColor(Context context, @NonNull Observer<Integer> observer) {
        NotificationViewModel model = new ViewModelProvider((ViewModelStoreOwner) context).get(NotificationViewModel.class);
        model.getDataTextColor().observe((LifecycleOwner) context, observer);
    }

}
