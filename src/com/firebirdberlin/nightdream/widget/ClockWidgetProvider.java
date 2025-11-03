package com.firebirdberlin.nightdream.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.firebirdberlin.nightdream.DataSource;
import com.firebirdberlin.nightdream.Graphics;
import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.services.DownloadWeatherService;
import com.firebirdberlin.nightdream.ui.ClockLayout;

import java.util.Calendar;
import java.util.Locale;

public class ClockWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "WidgetProvider";
    private static final String LOG_FILE_WEATHER_UPDATE = "nightdream_weather_update_log.txt";
    private AlarmManager mAlarmManager;
    private static final int RC_UPDATE = 0x13;

    private static ViewInfo prepareSourceView(Context context, WidgetDimension dimension, int appWidgetId) {

        final Dimension widgetSize = actualWidgetSize(context, dimension);

        // convert width/height from dip to pixels, otherwise widgetBitmap is blurry
        int widthPixel = Utility.dpToPx(context, widgetSize.width);
        int heightPixel = Utility.dpToPx(context, widgetSize.height);

        // load a view from resource
        LayoutInflater inflater = LayoutInflater.from(context);
        View container = inflater.inflate(R.layout.clock_widget_clock_layout, null);

        ClockLayout clockLayout = container.findViewById(R.id.clockLayout);

        updateClockLayoutSettings(context, appWidgetId, clockLayout, widgetSize);

        Configuration config = context.getResources().getConfiguration();
        clockLayout.updateLayoutForWidget(widthPixel, heightPixel, config);

        container.measure(
                View.MeasureSpec.makeMeasureSpec(widthPixel, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightPixel, View.MeasureSpec.EXACTLY)
        );
        container.layout(0, 0, widthPixel, heightPixel);

        return new ViewInfo(container, widthPixel, heightPixel);
    }

    private static String getAlarmClockEmoji(){
        return new String(Character.toChars(0x1F514));
    }

    private static TextView getNextAlarm(Context context, Settings settings){
        DataSource db = new DataSource(context);
        db.open();
        SimpleTime nextAlarm = db.getNextAlarmToSchedule();
        db.close();

        if (nextAlarm != null) {
            String nextAlarmString = String.format(
                    "%s %s",
                    getAlarmClockEmoji(),
                    Utility.getTimeFormatted(context, nextAlarm.getCalendar())
            );

            TextView alarmTime = new TextView(context);
            alarmTime.setTextColor(settings.secondaryColor);
            alarmTime.setGravity(Gravity.END);
            alarmTime.setText(nextAlarmString);
            return alarmTime;
        }
        return null;
    }


    private static void updateClockLayoutSettings(
            Context context, int appWidgetId, ClockLayout clockLayout, Dimension widgetDimension
    ) {
        SharedPreferences widgetPrefs = context.getSharedPreferences(
                String.format(Locale.ENGLISH, "preferences_widget_%d", appWidgetId),
                Context.MODE_PRIVATE
        );
        Settings settings = new Settings(context);
        int clockLayoutId = settings.getClockLayoutID(false);
        if (widgetPrefs.contains("clockLayout")) {
            clockLayoutId = Integer.parseInt(widgetPrefs.getString("clockLayout", "0"));
            clockLayoutId = settings.getValidatedClockLayoutID(clockLayoutId, false);
        }

        int glowRadius = settings.getGlowRadius(clockLayoutId);
        int textureId = settings.getTextureResId(clockLayoutId);
        boolean showAlarm = widgetPrefs.getBoolean("showAlarm", false);
        boolean showWeather = widgetDimension.height >= 130 && widgetDimension.width >= 130;
        boolean showDate = widgetDimension.height >= 130 && widgetDimension.width >= 130;
        if (clockLayoutId == ClockLayout.LAYOUT_ID_DIGITAL3) {
            showWeather = true;
        }

        clockLayout.setBackgroundColor(Color.TRANSPARENT);
        clockLayout.setLayout(clockLayoutId);
        clockLayout.setTypeface(settings.loadTypeface(clockLayoutId));
        clockLayout.setPrimaryColor(settings.clockColor, glowRadius, settings.clockColor, textureId, false);
        clockLayout.setSecondaryColor(settings.secondaryColor);
        clockLayout.setDateFormat(settings.dateFormat);
        clockLayout.setTimeFormat(settings.getTimeFormat(), settings.is24HourFormat());
        clockLayout.setShowDivider(settings.getShowDivider(clockLayoutId));
        clockLayout.showDate(showDate && widgetPrefs.getBoolean("showDate", true));

        clockLayout.setShowNotifications(false);
        clockLayout.showPollenExposure(false);
        clockLayout.setWeatherIconSizeFactor(settings.getWeatherIconSizeFactor(clockLayoutId));

        if (settings.shallShowWeather()) {
            if (settings.getWeatherAutoLocationEnabled()) {
                Location location = Utility.getLastKnownLocation(context);
                settings.setLocation(location);
            }

            DownloadWeatherService.start(context, settings);
        }

        // update weather date if not outdated
        if (settings.weatherEntry != null && settings.weatherEntry.isValid()) {
            clockLayout.setTemperature(
                    settings.showTemperature,
                    settings.showApparentTemperature,
                    settings.temperatureUnit
            );
            clockLayout.setWindSpeed(settings.showWindSpeed, settings.speedUnit);
            clockLayout.showWeather(showWeather && settings.shallShowWeather());
            clockLayout.setWeatherLocation(false);
            clockLayout.setWeatherIconMode(settings.weather_icon);
            clockLayout.update(settings.weatherEntry, true);
        } else {
            clockLayout.clearWeather();
        }

        TextView alarmTime = getNextAlarm(context, settings);
        if (showAlarm && alarmTime != null) {
            clockLayout.addView(alarmTime, 0);
        }

        {   // draw background
            int transparency = 255 - widgetPrefs.getInt("clockBackgroundTransparency", 100);

            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(30);
            shape.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            int color = Color.parseColor("#000000");

            int[] colors = {
                    Graphics.setColorWithAlpha(color, transparency),
                    Graphics.setColorWithAlpha(color, Math.max(0, transparency - 50))
            };
            shape.setColors(colors);
            clockLayout.setBackground(shape);
        }
    }

    private static Dimension actualWidgetSize(Context context, WidgetDimension dimension) {

        int width;
        int height;
        // detect screen orientation:
        //   portrait mode: width=minWidth, height=maxHeight,
        //   landscape mode: width=maxWidth, height=minHeight
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
        if (!Utility.isScreenOn(context)) {
            return;
        }
        // update all widget instances via intent
        final int[] appWidgetIds = getAppWidgetIds(context);
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, context, ClockWidgetProvider.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.sendBroadcast(intent);
    }

    public static int[] getAppWidgetIds(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        return appWidgetManager.getAppWidgetIds(new ComponentName(context, ClockWidgetProvider.class));
    }

    public static boolean hasWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final int[] widgetsIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, ClockWidgetProvider.class));
        return widgetsIds.length > 0;
    }

    private WidgetDimension widgetDimensionFromBundle(Bundle bundle) {
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
        // called when first widget instance is put to home screen
        super.onEnabled(context);
        Log.i(TAG, "onEnabled");
        scheduleUpdateClock(context, AppWidgetManager.getInstance(context).getAppWidgetIds(getComponentName(context)));
    }

    @Override
    public void onDisabled(Context context) {
        // when last instance was removed
        super.onDisabled(context);
        Log.i(TAG, "onDisabled");
        PendingIntent pendingIntent = getUpdateIntent(context, null);
        mAlarmManager.cancel(pendingIntent);
    }

    PendingIntent getUpdateIntent(Context context, int[] widgetIds) {
        Intent intent = new Intent(context, ClockWidgetProvider.class).setAction(
                AppWidgetManager.ACTION_APPWIDGET_UPDATE
        );
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        return Utility.getImmutableBroadcast(context, RC_UPDATE, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate()");

        if (appWidgetIds == null) {
            appWidgetIds = appWidgetManager.getAppWidgetIds(getComponentName(context));
        }
        scheduleUpdateClock(context, appWidgetIds);

        for (int widgetId : appWidgetIds) {
            Bundle bundle = appWidgetManager.getAppWidgetOptions(widgetId);

            WidgetDimension w = widgetDimensionFromBundle(bundle);
            updateWidget(context, appWidgetManager, widgetId, w);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager,
                              int appWidgetId, WidgetDimension dimension) {

        //Utility.logToFile(context, LOG_FILE_WEATHER_UPDATE, "updated widget");
        final PrepareBitmapTask task = new PrepareBitmapTask(context, appWidgetManager, appWidgetId, dimension);
        task.execute(context);
    }

    private void scheduleUpdateClock(Context context, int[] widgetIds) {
        Log.d(TAG, "scheduleUpdateClock()");

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.add(Calendar.MINUTE, 1);

        long millis = calendar.getTimeInMillis();
        PendingIntent pendingIntent = getUpdateIntent(context, widgetIds);
        mAlarmManager.setExact(AlarmManager.RTC, millis, pendingIntent);
    }

    private static ComponentName getComponentName(Context context) {
        return new ComponentName(context.getPackageName(), ClockWidgetProvider.class.getName());
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle bundle) {
        WidgetDimension w = widgetDimensionFromBundle(bundle);
        Log.d(TAG, String.format("onUpdate: widgetId=%d min width=%d max width=%d min height=%d max height=%d", appWidgetId, w.minWidth, w.maxWidth, w.minHeight, w.maxHeight));

        updateWidget(context, appWidgetManager, appWidgetId, w);
    }

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

    private static class PrepareBitmapTask extends AsyncTask<Context, Void, PrepareBitmapTask.TaskResult> {

        private final AppWidgetManager appWidgetManager;
        private final int appWidgetId;
        private final WidgetDimension dimension;
        private final Context applicationContext;

        static class TaskResult {
            final ViewInfo viewInfo;
            final RemoteViews remoteViews;

            TaskResult(ViewInfo viewInfo, RemoteViews remoteViews) {
                this.viewInfo = viewInfo;
                this.remoteViews = remoteViews;
            }
        }

        PrepareBitmapTask(Context context, AppWidgetManager appWidgetManager, int appWidgetId, WidgetDimension dimension) {
            super();
            this.applicationContext = context.getApplicationContext();
            this.appWidgetManager = appWidgetManager;
            this.appWidgetId = appWidgetId;
            this.dimension = dimension;
        }

        @Override
        protected TaskResult doInBackground(Context... contexts) {
            final ViewInfo sourceView = prepareSourceView(applicationContext, dimension, appWidgetId);

            RemoteViews updateViews = new RemoteViews(applicationContext.getPackageName(), R.layout.clock_widget);
            Intent intent = new Intent(applicationContext, NightDreamActivity.class);
            PendingIntent pendingIntent = Utility.getImmutableActivity(applicationContext, 0, intent);
            updateViews.setOnClickPendingIntent(R.id.clockWidgetImageView, pendingIntent);

            return new TaskResult(sourceView, updateViews);
        }

        private static Bitmap loadBitmapFromView(ViewInfo viewInfo) {
            Bitmap bitmap = null;
            View view = viewInfo.view;
            if (view == null) {
                return null;
            }

            int w = view.getWidth();
            int h = view.getHeight();

            if (w > 0 && h > 0) {
                bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                view.draw(canvas);

                if (w > viewInfo.widgetWidthPixel || h > viewInfo.widgetHeightPixel) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, viewInfo.widgetWidthPixel, viewInfo.widgetHeightPixel, true);
                }
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(TaskResult result) {
            if (result == null || result.viewInfo == null || result.remoteViews == null) {
                return;
            }

            Bitmap widgetBitmap = loadBitmapFromView(result.viewInfo);

            if (widgetBitmap == null) {
                return;
            }

            try {
                result.remoteViews.setImageViewBitmap(R.id.clockWidgetImageView, widgetBitmap);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Failed to set bitmap to RemoteViews", e);
                return;
            }

            try {
                appWidgetManager.updateAppWidget(appWidgetId, result.remoteViews);
            } catch (IllegalArgumentException ignore) {
            }

            System.gc(); //optional
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

    static final class WidgetDimension {
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

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"onReceive(): "+intent.getAction());

        mAlarmManager = ContextCompat.getSystemService(context, AlarmManager.class);
        if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction()))
            onUpdate(context, AppWidgetManager.getInstance(context), null);
        else
            super.onReceive(context, intent);
    }

}
