package com.firebirdberlin.nightdream.widget;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RemoteViews;

import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.services.WeatherService;
import com.firebirdberlin.nightdream.ui.ClockLayout;
import com.firebirdberlin.nightdream.ui.ClockLayoutPreviewPreference;

import java.util.Calendar;


public class ClockWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "WidgetProvider";
    private static Bitmap widgetBitmap;
    private static TimeReceiver timeReceiver;

    static void updateAllWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int widgetId : appWidgetIds) {

            // API 16 and up only
            Bundle bundle = appWidgetManager.getAppWidgetOptions(widgetId);

            WidgetDimension w = ClockWidgetProvider.widgetDimensionFromBundle(bundle);

            //Log.d(TAG, String.format("widgetId=%d minwidth=%d maxwidth=%d minheight=%d maxheight=%d", widgetId, w.minWidth, w.maxWidth, w.minHeight, w.maxHeight));

            updateWidget(context, appWidgetManager, widgetId, w);
        }
    }

    private static void updateWidget(Context context, AppWidgetManager appWidgetManager,
                                     int appWidgetId, WidgetDimension dimension) {

        final View sourceView = prepareSourceView(context, dimension);

        if (widgetBitmap != null) {
            sourceView.destroyDrawingCache();
            widgetBitmap.recycle();
            widgetBitmap = null;
            System.gc();
        }
        sourceView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        sourceView.setDrawingCacheEnabled(true);
        widgetBitmap = sourceView.getDrawingCache(false);

        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.clock_widget);
        updateViews.setImageViewBitmap(R.id.clockWidgetImageView, widgetBitmap);

        // click activates app
        Intent intent = new Intent(context, NightDreamActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        updateViews.setOnClickPendingIntent(R.id.clockWidgetImageView, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, updateViews);
    }

    private static View prepareSourceView(Context context, WidgetDimension dimension) {

        final Dimension widgetSize = actualWidgetSize(context, dimension);

        // convert width/height from dip to pixels, otherwise widgetBitmap is blurry
        int widthPixel = Utility.dpToPx(context, widgetSize.width);
        int heightPixel = Utility.dpToPx(context, widgetSize.height);

        // load a view from resource
        LayoutInflater inflater = LayoutInflater.from(context);
        View container = inflater.inflate(R.layout.clock_widget_clock_layout, null);

        ClockLayout clockLayout = (ClockLayout) container.findViewById(R.id.clockLayout);

        final boolean lightBackground = true;
        updateClockLayoutSettings(context, clockLayout, widgetSize);

        // round corners
        if (lightBackground) {
            GradientDrawable shape =  new GradientDrawable();
            shape.setCornerRadius(30);
            shape.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            int[] colors = {
                    Color.parseColor("#11000000"),
                    Color.parseColor("#AA000000")
            };
            shape.setColors(colors);
            clockLayout.setBackground(shape);
        }

        Configuration config = context.getResources().getConfiguration();
        clockLayout.updateLayoutForWidget(widthPixel, heightPixel, config);

        // give digital clock some padding
        if (clockLayout.isDigital()) {
            clockLayout.setPadding(15, 15, 15, 15);
        }

        Log.i(TAG, "widget height=" + heightPixel);

        // container.measure(viewWidth, viewHeight); // this wont work, use with makeMeasureSpec below
        container.measure(
                View.MeasureSpec.makeMeasureSpec(widthPixel, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightPixel, View.MeasureSpec.EXACTLY));
        container.layout(0, 0, widthPixel, heightPixel);

        // not needed obviously
        //sourceView.requestLayout();
        //sourceView.invalidate();

        return container;
    }

    private static void updateClockLayoutSettings(Context context, ClockLayout clockLayout,
                                                  Dimension widgetDimension) {
        boolean showAdditionalLines = widgetDimension.height >= 130 && widgetDimension.width >= 130;
        Settings settings = new Settings(context);

        clockLayout.setBackgroundColor(Color.TRANSPARENT);
        clockLayout.setLayout(settings.getClockLayoutID(false));
        clockLayout.setTypeface(settings.typeface);
        ClockLayoutPreviewPreference.PreviewMode previewMode = ClockLayoutPreviewPreference.PreviewMode.DAY;

        clockLayout.setPrimaryColor(previewMode == ClockLayoutPreviewPreference.PreviewMode.DAY ? settings.clockColor : settings.clockColorNight);
        clockLayout.setSecondaryColor(previewMode == ClockLayoutPreviewPreference.PreviewMode.DAY ? settings.secondaryColor : settings.secondaryColorNight);

        clockLayout.setDateFormat(settings.dateFormat);
        clockLayout.setTimeFormat(settings.timeFormat12h, settings.timeFormat24h);
        clockLayout.showDate(showAdditionalLines && settings.showDate);

        // update weather data via api if outdated
        if (WeatherService.shallUpdateWeatherData(settings)) {
            settings.setLastWeatherRequestTime(System.currentTimeMillis());
            WeatherService.start(context, settings.weatherCityID);
        }

        // update weather date if not outdated
        if (settings.weatherEntry != null
                && settings.weatherEntry.timestamp > -1L
                && settings.weatherEntry.ageMillis() <= 8 * 60 * 60 * 1000) {
            clockLayout.setTemperature(settings.showTemperature, settings.temperatureUnit);
            clockLayout.setWindSpeed(settings.showWindSpeed, settings.speedUnit);
            clockLayout.showWeather(showAdditionalLines && settings.showWeather);

            clockLayout.update(settings.weatherEntry);
        } else {
            clockLayout.clearWeather();
        }
    }

    private static Dimension actualWidgetSize(Context context, WidgetDimension dimension) {

        int width;
        int height;
        // detect screen orientation:
        // portrait mode: width=minWidth, height=maxHeight, landscape mode: width=maxWidth, height=minHeight
        final int orientation = context.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.i(TAG, "portrait");
            width = dimension.minWidth;
            height = dimension.maxHeight;
        } else {
            Log.i(TAG, "landscape");
            width = dimension.maxWidth;
            height = dimension.minHeight;
        }

        return new Dimension(width, height);
    }

    public static WidgetDimension widgetDimensionFromBundle(Bundle bundle) {

        // API 16 and up only
        //portrait mode: width=minWidth, height=maxHeight, landscape mode: width=maxWidth, height=minHeight
        int minWidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxWidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minHeight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxHeight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

        return new WidgetDimension(minWidth, minHeight, maxWidth, maxHeight);
    }

    public static int[] appWidgetIds(Context context, AppWidgetManager appWidgetManager) {

        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, ClockWidgetProvider.class));
        return appWidgetIds;

    }

    public static void updateAllWidgets(Context context) {
        // update all widget instances via intent
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final int[] appWidgetIds = ClockWidgetProvider.appWidgetIds(context, appWidgetManager);
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, context, ClockWidgetProvider.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.sendBroadcast(intent);
    }

    @Override
    public void onEnabled(Context context) {
        // call when first widget instance is put to home screen
        super.onEnabled(context);
        Log.d(TAG, "onEnabled");
    }

    @Override
    public void onDisabled(Context context) {
        // when last instance was removed
        super.onDisabled(context);
        unsetTimeTick(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.cancelAll();
        } else {
            // stop alarm
            stopAlarmManagerService(context);
        }

        // to be removed
        //ClockWidgetTimeTickService.stopService(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");

        setTimeTick(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scheduleJob(context);
        } else {
            scheduleAlarmManagerService(context);
        }

        // keep running a background service, which registers the ACTION_TIME_TICK broadcast receiver.
        // to be removed
        //ClockWidgetTimeTickService.startService(context);

        updateAllWidgets(context, appWidgetManager, appWidgetIds);
    }

    void setTimeTick(Context context) {
        if (timeReceiver != null) return;
        timeReceiver = new TimeReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        context.getApplicationContext().registerReceiver(timeReceiver, intentFilter);
    }

    void unsetTimeTick(Context context) {
        if (timeReceiver != null) {
            context.getApplicationContext().unregisterReceiver(timeReceiver);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void scheduleJob(Context context) {
        ComponentName serviceComponent = new ComponentName(context.getPackageName(),
                ClockWidgetJobService.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);

        builder.setPersisted(true);
        builder.setPeriodic(60000);
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        int jobResult = jobScheduler.schedule(builder.build());

        if (jobResult == JobScheduler.RESULT_SUCCESS){
            Log.d(TAG, "scheduled ClockWidgetJobService job successfully");
        }
    }

    private void scheduleAlarmManagerService(Context context) {
        final AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final int[] appWidgetIds = ClockWidgetProvider.appWidgetIds(context, appWidgetManager);
        Intent alarmIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        alarmIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

        final int ALARM_ID = 0;
        final int INTERVAL_MILLIS = 60000;

        PendingIntent removedIntent = PendingIntent.getBroadcast(context, ALARM_ID, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Log.d(TAG, "StartAlarm");

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MILLISECOND, INTERVAL_MILLIS);

        manager.cancel(removedIntent);
        manager.setRepeating(AlarmManager.ELAPSED_REALTIME, calendar.getTimeInMillis(), INTERVAL_MILLIS, pendingIntent);
    }

    private void stopAlarmManagerService(Context context) {

        final int ALARM_ID = 0;

        Intent alarmIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        final AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle bundle) {
        //Log.d(TAG, "onAppWidgetOptionsChanged");
        WidgetDimension w = ClockWidgetProvider.widgetDimensionFromBundle(bundle);
        Log.d(TAG, String.format("onUpdate: widgetId=%d minwidth=%d maxwidth=%d minheight=%d maxheight=%d", appWidgetId, w.minWidth, w.maxWidth, w.minHeight, w.maxHeight));

        updateWidget(context, appWidgetManager, appWidgetId, w);
    }

    public static final class Dimension {
        public final int width;
        public final int height;

        public Dimension(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    public static final class WidgetDimension {
        public final int minWidth;
        public final int minHeight;
        public final int maxWidth;
        public final int maxHeight;

        public WidgetDimension(int minWidth, int minHeight, int maxWidth, int maxHeight) {
            this.minWidth = minWidth;
            this.minHeight = minHeight;
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
        }
    }

    class TimeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {
            Log.d(TAG, "time tick");
            updateAllWidgets(context);
        }
    }

}
