package com.firebirdberlin.nightdream.widget;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
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

import java.util.Calendar;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ClockWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "WidgetProvider";
    private static final String LOG_FILE_WEATHER_UPDATE = "nightdream_weather_update_log.txt";
    private static TimeReceiver timeReceiver;
    private static ScreenReceiver screenReceiver;

    private static final class ViewInfo {
        public final View view;
        public final int widgetWidthPixel;
        public final int widgetHeightPixel;

        public ViewInfo(View view, int widgetWidthPixel, int widgetHeightPixel) {
            this.view = view;
            this.widgetWidthPixel = widgetWidthPixel;
            this.widgetHeightPixel = widgetHeightPixel;
        }
    }

    private static ViewInfo prepareSourceView(Context context, WidgetDimension dimension) {

        final Dimension widgetSize = actualWidgetSize(context, dimension);

        // convert width/height from dip to pixels, otherwise widgetBitmap is blurry
        int widthPixel = Utility.dpToPx(context, widgetSize.width);
        int heightPixel = Utility.dpToPx(context, widgetSize.height);

        // load a view from resource
        LayoutInflater inflater = LayoutInflater.from(context);
        View container = inflater.inflate(R.layout.clock_widget_clock_layout, null);

        ClockLayout clockLayout = (ClockLayout) container.findViewById(R.id.clockLayout);

        updateClockLayoutSettings(context, clockLayout, widgetSize);

        Configuration config = context.getResources().getConfiguration();
        clockLayout.updateLayoutForWidget(widthPixel, heightPixel, config);

        container.measure(
                View.MeasureSpec.makeMeasureSpec(widthPixel, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightPixel, View.MeasureSpec.EXACTLY)
        );
        container.layout(0, 0, widthPixel, heightPixel);

        return new ViewInfo(container, widthPixel, heightPixel);
    }

    private static void updateClockLayoutSettings(Context context, ClockLayout clockLayout,
                                                  Dimension widgetDimension) {
        boolean showAdditionalLines = widgetDimension.height >= 130 && widgetDimension.width >= 130;
        Settings settings = new Settings(context);

        clockLayout.setBackgroundColor(Color.TRANSPARENT);
        clockLayout.setLayout(settings.getClockLayoutID(false));
        clockLayout.setTypeface(settings.typeface);
        clockLayout.setPrimaryColor(settings.clockColor);
        clockLayout.setSecondaryColor(settings.secondaryColor);
        clockLayout.setDateFormat(settings.dateFormat);
        clockLayout.setTimeFormat(settings.timeFormat12h, settings.timeFormat24h);
        clockLayout.setShowDivider(settings.showDivider);
        clockLayout.showDate(showAdditionalLines && settings.showDate);

        // update weather data via api if outdated
        if (WeatherService.shallUpdateWeatherData(settings)) {
            settings.setLastWeatherRequestTime(System.currentTimeMillis());
            WeatherService.start(context, settings.weatherCityID);
            //Utility.logToFile(context, LOG_FILE_WEATHER_UPDATE, "updated weather");
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

        {   // draw background
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(30);
            shape.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            int[] colors = {
                    Color.parseColor("#11000000"),
                    Color.parseColor("#AA000000")
            };
            shape.setColors(colors);
            clockLayout.setBackground(shape);
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

    public static void updateAllWidgets(Context context) {
        // update all widget instances via intent
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final int[] appWidgetIds = getAppWidgetIds(context, appWidgetManager);
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, context, ClockWidgetProvider.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.sendBroadcast(intent);
    }

    public static int[] getAppWidgetIds(Context context, AppWidgetManager appWidgetManager) {
        return appWidgetManager.getAppWidgetIds(new ComponentName(context, ClockWidgetProvider.class));
    }

    public WidgetDimension widgetDimensionFromBundle(Bundle bundle) {

        // API 16 and up only
        //portrait mode: width=minWidth, height=maxHeight, landscape mode: width=maxWidth, height=minHeight
        int minWidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxWidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minHeight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxHeight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

        return new WidgetDimension(minWidth, minHeight, maxWidth, maxHeight);
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
            if (jobScheduler != null) {
                jobScheduler.cancel(0);
            }
        } else {
            // stop alarm
            stopAlarmManagerService(context);
        }

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");
        setTimeTick(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ClockWidgetJobService.schedule(context);
        } else {
            scheduleAlarmManagerService(context);
        }

        for (int widgetId : appWidgetIds) {

            // API 16 and up only
            Bundle bundle = appWidgetManager.getAppWidgetOptions(widgetId);

            WidgetDimension w = widgetDimensionFromBundle(bundle);
            updateWidget(context, appWidgetManager, widgetId, w);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager,
                              int appWidgetId, WidgetDimension dimension) {

        //Utility.logToFile(context, LOG_FILE_WEATHER_UPDATE, "updated widget");

        final PrepareBitmapTask task = new PrepareBitmapTask(appWidgetManager, appWidgetId, dimension);
        task.execute(context);
    }

    void setTimeTick(Context context) {
        if (timeReceiver != null) return;
        Log.d(TAG, "setTimeTick()");
        if (screenReceiver != null) {
            context.getApplicationContext().unregisterReceiver(screenReceiver);
            screenReceiver = null;
        }
        timeReceiver = new TimeReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        context.getApplicationContext().registerReceiver(timeReceiver, intentFilter);
    }

    void unsetTimeTick(Context context) {
        Log.d(TAG, "unsetTimeTick");
        if (timeReceiver != null) {
            context.getApplicationContext().unregisterReceiver(timeReceiver);
            timeReceiver = null;
        }
        if (screenReceiver == null) {
            screenReceiver = new ScreenReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_SCREEN_ON);

            context.getApplicationContext().registerReceiver(screenReceiver, intentFilter);
        }
    }

    private void scheduleAlarmManagerService(Context context) {
        final AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final int[] appWidgetIds = ClockWidgetProvider.getAppWidgetIds(context, appWidgetManager);
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
        if (manager == null) {
            return;
        }
        manager.cancel(pendingIntent);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle bundle) {
        //Log.d(TAG, "onAppWidgetOptionsChanged");
        WidgetDimension w = widgetDimensionFromBundle(bundle);
        Log.d(TAG, String.format("onUpdate: widgetId=%d minwidth=%d maxwidth=%d minheight=%d maxheight=%d", appWidgetId, w.minWidth, w.maxWidth, w.minHeight, w.maxHeight));

        updateWidget(context, appWidgetManager, appWidgetId, w);
    }

    private static class PrepareBitmapTask extends AsyncTask<Context, Void, RemoteViews> {

        private AppWidgetManager appWidgetManager;
        private int appWidgetId;
        private WidgetDimension dimension;

        PrepareBitmapTask(AppWidgetManager appWidgetManager, int appWidgetId,
                          WidgetDimension dimension) {
            super();
            this.appWidgetManager = appWidgetManager;
            this.appWidgetId = appWidgetId;
            this.dimension = dimension;
        }

        @Override
        protected RemoteViews doInBackground(Context... contexts) {
            Context context = contexts[0];
            final ViewInfo sourceView = prepareSourceView(context, dimension);
            Bitmap widgetBitmap = loadBitmapFromView(sourceView);

            RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.clock_widget);
            if (widgetBitmap == null) {
                return null;
            }

            try {
                updateViews.setImageViewBitmap(R.id.clockWidgetImageView, widgetBitmap);
            } catch (IllegalArgumentException e) {
                return null;
            }

            // click activates app
            Intent intent = new Intent(context, NightDreamActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            updateViews.setOnClickPendingIntent(R.id.clockWidgetImageView, pendingIntent);

            System.gc();
            return updateViews;
        }

        private Bitmap loadBitmapFromView(ViewInfo viewInfo) {
            Bitmap bitmap = null;
            View view = viewInfo.view;
            if (view != null) {
                view.setDrawingCacheEnabled(true);
                view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
                try {
                    view.buildDrawingCache();
                    Bitmap drawingCache = view.getDrawingCache();
                    if (drawingCache != null) {
                        // assert that the bitmap is not larger than the widget area (bitmaps larger than the screen cause IllegalArgumentException in RemoteViews)
                        if (view.getWidth() > viewInfo.widgetWidthPixel || view.getHeight() > viewInfo.widgetHeightPixel) {
                            // down-scale the bitmap
                            bitmap = Bitmap.createScaledBitmap(drawingCache, viewInfo.widgetWidthPixel, viewInfo.widgetHeightPixel, true);
                        } else {
                            bitmap = Bitmap.createBitmap(drawingCache);
                        }
                    }
                } finally {
                    view.setDrawingCacheEnabled(false);
                }

                if (bitmap == null) {
                    bitmap = createLargeBitmapFromView(viewInfo);
                }
            }

            return bitmap;
        }

        /**
         * fallback if getDrawingCache() returns null
         */
        private Bitmap createLargeBitmapFromView(ViewInfo viewInfo) {
            View view = viewInfo.view;

            int w = view.getWidth();
            int h = view.getHeight();

            Bitmap bitmap = null;
            if (w > 0 && h > 0) {
                bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                view.draw(canvas);

                // assert that the bitmap is not larger than the widget area (bitmaps larger than the screen cause IllegalArgumentException in RemoteViews)
                if (view.getWidth() > viewInfo.widgetWidthPixel || view.getHeight() > viewInfo.widgetHeightPixel) {
                    // down-scale the bitmap
                    bitmap = Bitmap.createScaledBitmap(bitmap, viewInfo.widgetWidthPixel, viewInfo.widgetHeightPixel, true);
                }
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(RemoteViews updateViews) {
            if (updateViews == null) return;
            try {
                appWidgetManager.updateAppWidget(appWidgetId, updateViews);
            } catch (IllegalArgumentException ignore) {

            }
        }
    }

    public static final class Dimension {
        public final int width;
        public final int height;

        Dimension(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    final class WidgetDimension {
        final int minWidth;
        final int minHeight;
        final int maxWidth;
        final int maxHeight;

        WidgetDimension(int minWidth, int minHeight, int maxWidth, int maxHeight) {
            this.minWidth = minWidth;
            this.minHeight = minHeight;
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
        }
    }

    class TimeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "time tick");
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                unsetTimeTick(context);
            } else if (Intent.ACTION_TIME_TICK.equals(action)) {
                updateAllWidgets(context);
            }
        }
    }

    class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "screen ON");
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                updateAllWidgets(context);
            }
        }
    }

}
