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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RemoteViews;

import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.ui.ClockLayout;


public class ClockWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "WidgetProvider";

    private PendingIntent alarmIntent = null;

    @Override
    public void onEnabled(Context context) {
        // call when first widget instance is put to home screen
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        // when last instance was removed
        super.onDisabled(context);
    }



    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        Log.d(TAG, "onUpdate");

        setupClockUpdateService(context);

        updateAllWidgets(context, appWidgetManager, appWidgetIds);

    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle bundle) {

        Log.d(TAG, "onAppWidgetOptionsChanged");
        WidgetDimension w = ClockWidgetProvider.widgetDimensionFromBundle(bundle);
        Log.d(TAG, String.format("onUpdate: widgetId=%d minwidth=%d maxwidth=%d minheight=%d maxheight=%d", appWidgetId, w.minWidth, w.maxWidth, w.minHeight, w.maxHeight));

        updateWidget(context, appWidgetManager, appWidgetId, w);

    }

    static void updateAllWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int widgetId : appWidgetIds) {

            // API 16 and up only
            Bundle bundle = appWidgetManager.getAppWidgetOptions(widgetId);

            WidgetDimension w = ClockWidgetProvider.widgetDimensionFromBundle(bundle);

            Log.d(TAG, String.format("widgetId=%d minwidth=%d maxwidth=%d minheight=%d maxheight=%d", widgetId, w.minWidth, w.maxWidth, w.minHeight, w.maxHeight));

            updateWidget(context, appWidgetManager, widgetId, w);
        }
    }

    private static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, WidgetDimension dimension) {

        //final View sourceView = prepareSourceViewTest(context, dimension);
        final View sourceView = prepareSourceView(context, dimension);


        sourceView.setDrawingCacheEnabled(true);
        Bitmap widgetBitmap = sourceView.getDrawingCache();
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
        //View container = rootView.findViewById(R.id.previewContainer);
        //clockLayout.setScaleFactor(.8f);

        // TODO: init all clockLayout params like in ClockLayoutPreviewPreference.updateView()
        clockLayout.setLayout(ClockLayout.LAYOUT_ID_DIGITAL);
        //clockLayout.setLayout(ClockLayout.LAYOUT_ID_ANALOG4);
        Configuration config = context.getResources().getConfiguration();

        clockLayout.updateLayout(widgetSize.width, config); // use dip width here
        clockLayout.requestLayout();

        // sourceView.measure(viewWidth, viewHeight); // this wont work, use with makeMeasureSpec below
        clockLayout.measure(
                View.MeasureSpec.makeMeasureSpec(widthPixel, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightPixel, View.MeasureSpec.EXACTLY));
        clockLayout.layout(0, 0, widthPixel, heightPixel);

        // not needed obviously
        //sourceView.requestLayout();
        //sourceView.invalidate();

        //center after layout
        /*
        if (clockLayout.getWidth() > 0 && clockLayout.getWidth() < viewSize.width) {
            //clockLayout.setLeft((viewSize.width - clockLayout.getWidth()) / 2);
            int left = (viewSize.width - clockLayout.getWidth()) / 2;
            int top = (viewSize.height - clockLayout.getHeight()) / 2;
            if (Build.VERSION.SDK_INT >= 11) {
                clockLayout.setTranslationX(left);
                clockLayout.setTranslationY(-20);
            } else {
                //?
                //clockLayout.setPadding(0, 0, 0, 0);
            }
        }
        */

        return clockLayout;
    }


    private static View prepareSourceViewTest(Context context, WidgetDimension dimension) {

        final Dimension widgetSize = actualWidgetSize(context, dimension);

        // convert width/height from dip to pixels, otherwise widgetBitmap is blurry
        int widthPixel = Utility.dpToPx(context, widgetSize.width);
        int heightPixel = Utility.dpToPx(context, widgetSize.height);

        // load a view from resource
        LayoutInflater inflater = LayoutInflater.from(context);

        View sourceView = inflater.inflate(R.layout.widget_test_content, null);

        // sourceView.measure(viewWidth, viewHeight); // this wont work, use with makeMeasureSpec below
        sourceView.measure(
                View.MeasureSpec.makeMeasureSpec(widthPixel, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightPixel, View.MeasureSpec.EXACTLY));
        sourceView.layout(0, 0, widthPixel, heightPixel);

        // not needed obviously
        //sourceView.requestLayout();
        //sourceView.invalidate();

        return sourceView;
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

    private void setupClockUpdateService(Context context) {
        final AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final Intent intent = new Intent(context, UpdateService.class);

        if (alarmIntent == null) {
            alarmIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }

        manager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), 60000, alarmIntent);
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

    public static WidgetDimension widgetDimensionFromBundle(Bundle bundle) {

        // API 16 and up only
        //portrait mode: width=minWidth, height=maxHeight, landscape mode: width=maxWidth, height=minHeight
        int minwidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxwidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minheight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxheight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

        return new WidgetDimension(minwidth, minheight, maxwidth, maxheight);
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

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            int[] appWidgetIds = ClockWidgetProvider.appWidgetIds(this, appWidgetManager);

            updateAllWidgets(this, appWidgetManager, appWidgetIds);

        }
    }

    public static int[] appWidgetIds(Context context, AppWidgetManager appWidgetManager) {

        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, ClockWidgetProvider.class));
        return appWidgetIds;

    }

    public static void updateConfigurationChange() {

    }

}
