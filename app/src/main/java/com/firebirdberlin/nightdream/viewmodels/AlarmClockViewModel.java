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