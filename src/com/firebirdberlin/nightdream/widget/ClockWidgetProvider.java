package com.firebirdberlin.nightdream.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RemoteViews;

import com.firebirdberlin.nightdream.CustomAnalogClock;
import com.firebirdberlin.nightdream.CustomAnalogClock4;
import com.firebirdberlin.nightdream.CustomDigitalClock;
import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.ui.AutoAdjustTextView;
import com.firebirdberlin.nightdream.ui.ClockLayout;

import java.util.HashMap;
import java.util.Map;

import static com.firebirdberlin.nightdream.R.id.clock;
import static com.firebirdberlin.nightdream.R.id.clockLayoutContainer;

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

        for (int widgetId : appWidgetIds) {

            Dimension viewSize = widgetDimensions.get(widgetId);
            if (viewSize == null) {
                Log.d(TAG, "onUpdate: widget dimension is NULL");
                viewSize = new Dimension(150, 150);
            }

            renderClockWidget(context, appWidgetManager, widgetId, viewSize);
        }
    }

    private static void renderClockWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Dimension viewSize) {

        //renderClockWidgetTest1(context, appWidgetManager, appWidgetId, viewSize); //works
        renderClockWidgetTest2(context, appWidgetManager, appWidgetId, viewSize); //works
    }

    private static void renderClockWidgetTest1(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Dimension viewSize) {

        //View clockView = new CustomAnalogClock(context);
        //AutoAdjustTextView clockView = new CustomDigitalClock(context);
        //View clockView = new CustomAnalogClock4(context);

        View clockLayout = createClockViewTest1(context); // this works

        Log.d(TAG, String.format("view size: w=%d h=%d", viewSize.width, viewSize.height));

        clockLayout.measure(viewSize.width, viewSize.height);
        clockLayout.layout(0, 0, viewSize.width, viewSize.height);
        clockLayout.setDrawingCacheEnabled(true);
        Bitmap clockBitmap = clockLayout.getDrawingCache();
        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.clock_widget);
        updateViews.setImageViewBitmap(R.id.clockWidgetImageView, clockBitmap);

        // click activates app
        Intent intent = new Intent(context, NightDreamActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        updateViews.setOnClickPendingIntent(R.id.clockWidgetImageView, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, updateViews);
    }

    private static void renderClockWidgetTest2(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Dimension viewSize) {

        //View clockView = new CustomAnalogClock(context);
        //AutoAdjustTextView clockView = new CustomDigitalClock(context);
        //View clockView = new CustomAnalogClock4(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        View rootView = inflater.inflate(R.layout.clock_widget_clock_layout, null);
        ClockLayout clockLayout = (ClockLayout) rootView.findViewById(R.id.clockLayout);
        View container = rootView.findViewById(R.id.previewContainer);

        clockLayout.setScaleFactor(1f);
        if (Build.VERSION.SDK_INT >= 11) {
            clockLayout.setTranslationX(0);
            clockLayout.setTranslationY(0);
        } else {
            clockLayout.setPadding(0, 0, 0, 0);
        }
        //clockLayout.updateLayout(viewSize.width, config);
        clockLayout.setVisibility(View.VISIBLE);

        Log.d(TAG, String.format("view size: w=%d h=%d", viewSize.width, viewSize.height));


        // TODO: init all clockLayout params like in ClockLayoutPreviewPreference.updateView()
        //Utility utility = new Utility(context);
        //Point size = utility.getDisplaySize();
        clockLayout.setLayout(ClockLayout.LAYOUT_ID_DIGITAL);
        Configuration config = context.getResources().getConfiguration();
        clockLayout.updateLayout(viewSize.width, config);
        clockLayout.requestLayout();
        clockLayout.invalidate();
        //


        rootView.measure(viewSize.width, viewSize.height);



        rootView.layout(0, 0, viewSize.width, viewSize.height);

        Log.d(TAG, "clockLayout.getWidth()=" + clockLayout.getWidth() + ", container width=" + container.getWidth());

        //center after layout
        /*
        if (clockLayout.getWidth() > 0 && clockLayout.getWidth() < viewSize.width) {
            clockLayout.setLeft((viewSize.width - clockLayout.getWidth()) / 2);
        }
        */

        rootView.setDrawingCacheEnabled(true);
        Bitmap clockBitmap = rootView.getDrawingCache();
        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.clock_widget);
        updateViews.setImageViewBitmap(R.id.clockWidgetImageView, clockBitmap);

        // click activates app
        Intent intent = new Intent(context, NightDreamActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        updateViews.setOnClickPendingIntent(R.id.clockWidgetImageView, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, updateViews);
    }

    private static View createClockViewTest1(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View clockLayout = inflater.inflate(R.layout.clock_layout, null);
        AutoAdjustTextView clock = (AutoAdjustTextView) clockLayout.findViewById(R.id.clock);
        AutoAdjustTextView date = (AutoAdjustTextView) clockLayout.findViewById(R.id.date);
        Settings settings = new Settings(context);
        if (clock != null) {

            Typeface tf = settings.typeface;
            //Log.d(TAG, "#### typeface: " + tf.toString());
            clock.setTypeface(settings.typeface);

            Log.d(TAG, "#### clockColor: " + settings.clockColor);

            //day/night?
            //int accentColor = (mode == 0) ? settings.clockColorNight : settings.clockColor;
            //int textColor = (mode == 0) ? settings.secondaryColorNight : settings.secondaryColor;
            int accentColor = settings.clockColor;
            int textColor = settings.secondaryColor;
            clock.setTextColor(accentColor);

        }

        if (date != null) {
            CustomDigitalClock tdate = (CustomDigitalClock) date;
            tdate.setFormat12Hour(settings.dateFormat);
            tdate.setFormat24Hour(settings.dateFormat);
        }

        return clockLayout;
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
