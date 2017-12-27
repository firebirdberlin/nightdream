package com.firebirdberlin.nightdream.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RemoteViews;

import com.firebirdberlin.nightdream.CustomAnalogClock;
import com.firebirdberlin.nightdream.CustomAnalogClock4;
import com.firebirdberlin.nightdream.CustomDigitalClock;
import com.firebirdberlin.nightdream.R;

import java.util.HashMap;
import java.util.Map;

public class ClockWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "ClockWidgetProvider";

    static final Map<Integer, Dimension> WIDGET_DIMENSIONS = new HashMap<>();

    private PendingIntent alarmIntent = null;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        Log.d(TAG, "onUpdate");

        setupClockUpdateService(context);

        renderAllClockWidgets(context, appWidgetManager, appWidgetIds, WIDGET_DIMENSIONS);

    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle bundle) {
        Log.d(TAG, "onAppWidgetOptionsChanged");

        int minwidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxwidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minheight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxheight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

        //just take min values
        Dimension viewSize = new Dimension(minwidth, minheight);
        WIDGET_DIMENSIONS.put(appWidgetId, viewSize);

        Log.i(TAG, String.format("widget size: %d %d %d %d", minwidth, maxwidth, minheight, maxheight));

        renderClockWidget(context, appWidgetManager, appWidgetId, viewSize);
    }

    private void setupClockUpdateService(Context context) {
        final AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final Intent intent = new Intent(context, UpdateService.class);

        if (alarmIntent == null) {
            alarmIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }

        manager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), 60000, alarmIntent);
    }

    static void renderAllClockWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, Map<Integer, Dimension> widgetDimensions) {
        final int n = appWidgetIds.length;
        for (int i = 0; i < n; i++) {
            int currentWidgetId = appWidgetIds[i];


            Dimension viewSize = widgetDimensions.get(currentWidgetId);
            if (viewSize == null) {
                Log.d(TAG, "onUpdate: widget dimension is NULL");
                viewSize = new Dimension(150, 150);
            }

            renderClockWidget(context, appWidgetManager, currentWidgetId, viewSize);
        }
    }

    private static void renderClockWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Dimension viewSize) {
        //View clockView = new CustomAnalogClock(context);
        //View clockView = new CustomDigitalClock(context);
        View clockView = new CustomAnalogClock4(context);

        //LayoutInflater inflater = LayoutInflater.from(context);
        //View clockView = inflater.inflate(R.layout.clock_layout, null);

        clockView.measure(viewSize.width, viewSize.height);
        clockView.layout(0, 0, viewSize.width, viewSize.height);
        clockView.setDrawingCacheEnabled(true);
        Bitmap clockBitmap = clockView.getDrawingCache();
        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.clock_widget);
        updateViews.setImageViewBitmap(R.id.clockWidgetImageView, clockBitmap);
        appWidgetManager.updateAppWidget(appWidgetId, updateViews);
    }


    public static final class Dimension {
        public final int width;
        public final int height;

        public Dimension(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    public static class UpdateService extends Service {

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.d(TAG, "UpdateService.onStartCommand");

            updateWidgets();

            return super.onStartCommand(intent, flags, startId);
        }

        private void updateWidgets() {
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            int[] appWidgetIds = manager.getAppWidgetIds(new ComponentName(this, ClockWidgetProvider.class));
            ClockWidgetProvider.renderAllClockWidgets(this, manager, appWidgetIds, ClockWidgetProvider.WIDGET_DIMENSIONS);
        }
    }
}
