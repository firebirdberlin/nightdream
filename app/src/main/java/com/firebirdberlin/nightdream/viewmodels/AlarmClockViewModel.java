package com.firebirdberlin.nightdream.viewmodels;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.firebirdberlin.nightdream.models.SimpleTime;


public class AlarmClockViewModel extends ViewModel {
    private static final MutableLiveData<SimpleTime> nextAlarm = new MutableLiveData<>();

    public static void setNextAlarm(SimpleTime time) {
        nextAlarm.setValue(time);
    }

    private MutableLiveData<SimpleTime> getNextAlarm() {
        return nextAlarm;
    }

    public static void observeNextAlarm(Context context, @NonNull Observer<SimpleTime> observer) {
        AlarmClockViewModel model = new ViewModelProvider(
                (ViewModelStoreOwner) context
        ).get(AlarmClockViewModel.class);
        model.getNextAlarm().observe((LifecycleOwner) context, observer);
    }

}