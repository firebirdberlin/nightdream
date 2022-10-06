package com.firebirdberlin.nightdream.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import com.firebirdberlin.nightdream.DataSource;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.SetAlarmClockActivity;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.services.ScreenWatcherService;

import java.util.Calendar;
import java.util.Locale;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AlarmClockWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "AlarmClockWidget";

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
        db.close();

        String text = "";
        if (next != null) {
            Calendar cal = next.getCalendar();
            text = Utility.getTimeFormatted(context, cal);
        }
        if (text.isEmpty()) {
            text = context.getResources().getString(R.string.no_alarm_set);
        }
        views.setTextViewText(R.id.alarm_clock_text_view, text);

        Intent intent = new Intent(context, SetAlarmClockActivity.class);
        PendingIntent pendingIntent = Utility.getImmutableActivity(context, 0, intent);
        views.setOnClickPendingIntent(R.id.alarm_clock_text_view, pendingIntent);
        SharedPreferences widgetPrefs = context.getSharedPreferences(
                String.format(Locale.ENGLISH, "preferences_alarm_clock_widget_%d", appWidgetId),
                Context.MODE_PRIVATE
        );
        int backgroundColor = widgetPrefs.getInt(
                "backgroundColor", Color.parseColor("#80000000")
        );
        int textSize = widgetPrefs.getInt("textSize", 25);
        int foregroundColor = Utility.getContrastColor(backgroundColor);
        views.setInt(
                R.id.alarm_clock_background, "setBackgroundColor", backgroundColor
        );
        views.setInt(
                R.id.alarm_clock_text_view, "setTextColor", foregroundColor
        );
        views.setFloat(
                R.id.alarm_clock_text_view, "setTextSize", textSize
        );

        // Tell the AppWidgetManager to perform an update on the current app widget.
        appWidgetManager.updateAppWidget(appWidgetId, views);

    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle bundle) {
        updateWidget(context, appWidgetManager, appWidgetId);
    }
}
