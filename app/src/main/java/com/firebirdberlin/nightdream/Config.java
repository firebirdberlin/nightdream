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

package com.firebirdberlin.nightdream;

public class Config {

    public final static double NOISE_AMPLITUDE_WAKE  = 250.;
    public final static double NOISE_AMPLITUDE_SLEEP = 2.f * NOISE_AMPLITUDE_WAKE;
    public final static String ACTION_NOTIFICATION_LISTENER = "com.firebirdberlin.nightdream.NOTIFICATION_LISTENER";
    public final static String ACTION_NOTIFICATION_APPS_LISTENER = "com.firebirdberlin.nightdream.NOTIFICATION_APPS_LISTENER";
    public final static String ACTION_STOP_BACKGROUND_SERVICE = "com.firebirdberlin.nightdream.ACTION_STOP_BACKGROUND_SERVICE";
    public final static String ACTION_RADIO_STREAM_SWITCH_TO_RADIO_MODE = "com.firebirdberlin.nightdream.ACTION_RADIO_STREAM_SWITCH_TO_RADIO_MODE";

    public final static int NOTIFICATION_ID_DISMISS_ALARMS = 1338;
    public final static int NOTIFICATION_ID_FOREGROUND_SERVICES = 1339;
    public final static String NOTIFICATION_CHANNEL_ID_ALARMS = "com.firebirdberlin.nightdream.notification_channel_alarms";
    public final static String NOTIFICATION_CHANNEL_ID_RADIO = "com.firebirdberlin.nightdream.notification_channel_radio";
    public final static String NOTIFICATION_CHANNEL_ID_SERVICES = "com.firebirdberlin.nightdream.notification_channel_services";
    public final static String NOTIFICATION_CHANNEL_ID_DEVMSG = "com.firebirdberlin.nightdream.notification_channel_devmsg";

    public final static int JOB_ID_ALARM_NOTIFICATION = 2000;

}
