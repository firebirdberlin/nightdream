package com.firebirdberlin.nightdream.widget;

import static android.text.format.DateFormat.getBestDateTimePattern;
import static android.text.format.DateFormat.is24HourFormat;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import com.firebirdberlin.nightdream.DataSource;
import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.SetAlarmClockActivity;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.receivers.WakeUpReceiver;
import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.ScreenWatcherService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AlarmClockWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "WidgetProvider";

    public static void updateAllWidgets(Context context) {
        if (!Utility.isScreenOn(context)) {
            return;
        }
        // update all widget instances via intent
        final int[] appWidgetIds = getAppWidgetIds(context);
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, context, AlarmClockWidgetProvider.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.sendBroadcast(intent);
    }

    public static int[] getAppWidgetIds(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        return appWidgetManager.getAppWidgetIds(new ComponentName(context, AlarmClockWidgetProvider.class));
    }

    public static boolean hasWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final int[] widgetsIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, AlarmClockWidgetProvider.class));
        return widgetsIds.length > 0;
    }

    @Override
    public void onEnabled(Context context) {
        // called when first widget instance is put to home screen
        super.onEnabled(context);
        Log.i(TAG, "onEnabled");
    }

    @Override
    public void onDisabled(Context context) {
        // when last instance was removed
        super.onDisabled(context);
        Log.i(TAG, "onDisabled");
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");
        ScreenWatcherService.conditionallyStart(context);

        for (int widgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId);
        }
    }

    private void updateWidget(
            Context context, AppWidgetManager appWidgetManager, int appWidgetId
    ) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.alarm_clock_widget);

        DataSource db = new DataSource(context);
        db.open();
        SimpleTime next = db.getNextAlarmToSchedule();

        String text = "no alarm set";
        if (next != null) {
            Calendar cal = next.getCalendar();
            text = getTimeFormatted(context, cal);
        }
        views.setTextViewText(R.id.alarm_clock_text_view, text);
        db.close();


        Intent intent = new Intent(context, SetAlarmClockActivity.class);
        PendingIntent pendingIntent = Utility.getImmutableActivity(context, 0, intent);
        views.setOnClickPendingIntent(R.id.alarm_clock_text_view, pendingIntent);

        // Tell the AppWidgetManager to perform an update on the current app widget.
        appWidgetManager.updateAppWidget(appWidgetId, views);

    }

    private String getTimeFormatted(Context context, Calendar calendar) {
        Calendar now_in_one_week = Calendar.getInstance();
        now_in_one_week.add(Calendar.DAY_OF_MONTH, 7);
        if (calendar.after(now_in_one_week)) {
            return "";
        }
        String localPattern;
        if (Build.VERSION.SDK_INT >= 18) {
            if (is24HourFormat(context)) {
                localPattern = getBestDateTimePattern(Locale.getDefault(), "EE HH:mm");
            } else {
                localPattern = getBestDateTimePattern(Locale.getDefault(), "EE hh:mm a");
            }
        } else {
            DateFormat formatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
            localPattern = ((SimpleDateFormat) formatter).toLocalizedPattern();
        }

        SimpleDateFormat hourDateFormat = new SimpleDateFormat(localPattern, Locale.getDefault());
        return hourDateFormat.format(calendar.getTime());
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle bundle) {
        updateWidget(context, appWidgetManager, appWidgetId);
    }

}
