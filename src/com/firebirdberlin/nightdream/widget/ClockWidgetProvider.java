package com.firebirdberlin.nightdream.widget;

import android.app.PendingIntent;
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
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RemoteViews;

import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.ui.ClockLayout;
import com.firebirdberlin.nightdream.ui.ClockLayoutPreviewPreference;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;


public class ClockWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "WidgetProvider";
    private static Bitmap widgetBitmap;
    private static TimeReceiver timeReceiver;

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

        if (widgetBitmap != null) {
            sourceView.destroyDrawingCache();
            widgetBitmap.recycle();
            widgetBitmap = null;
            System.gc();
        }
        sourceView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        sourceView.setDrawingCacheEnabled(true);
        widgetBitmap = sourceView.getDrawingCache(true);

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

        // this causes a leak in view.getDrawingCache() !!
        updateClockLayoutSettings(context, clockLayout, lightBackground);

        if (widgetSize.height < 100) {
            // widget has only height of one cell -> hide weather anf for analog also date
            clockLayout.showWeather(false);
            if (!clockLayout.isDigital()) {
                clockLayout.showDate(false);
            }
        }

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

        Utility utility = new Utility(context);
        Point size = utility.getDisplaySize();
        Log.d(TAG, "display size ###" + size.x);
        Configuration config = context.getResources().getConfiguration();

        // causes memory leak for analog clock
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

    private static void updateClockLayoutSettings(Context context, ClockLayout clockLayout, boolean lightBackground) {
        Settings settings = new Settings(context);

        //textViewPurchaseHint.setVisibility(View.GONE);
        clockLayout.setBackgroundColor(Color.TRANSPARENT);
        clockLayout.setLayout(settings.getClockLayoutID(false));
        clockLayout.setTypeface(settings.typeface);
        ClockLayoutPreviewPreference.PreviewMode previewMode = ClockLayoutPreviewPreference.PreviewMode.DAY;

        clockLayout.setPrimaryColor(previewMode == ClockLayoutPreviewPreference.PreviewMode.DAY ? settings.clockColor : settings.clockColorNight);
        clockLayout.setSecondaryColor(previewMode == ClockLayoutPreviewPreference.PreviewMode.DAY ? settings.secondaryColor : settings.secondaryColorNight);


        clockLayout.setDateFormat(settings.dateFormat);
        clockLayout.setTimeFormat(settings.timeFormat12h, settings.timeFormat24h);
        clockLayout.showDate(settings.showDate);

        clockLayout.setTemperature(settings.showTemperature, settings.temperatureUnit);
        clockLayout.setWindSpeed(settings.showWindSpeed, settings.speedUnit);
        clockLayout.showWeather(settings.showWeather);

        // causes memory leak in getdrawingcache
        WeatherEntry entry = getWeatherEntry(settings);
        clockLayout.update(entry);
    }

    private static WeatherEntry getWeatherEntry(Settings settings) {
        WeatherEntry entry = settings.weatherEntry;
        if ( entry.timestamp ==  -1L) {
            entry.setFakeData();
        }
        return entry;
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
        int minwidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxwidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minheight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxheight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

        return new WidgetDimension(minwidth, minheight, maxwidth, maxheight);
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
        if (timeReceiver != null) {
            context.getApplicationContext().unregisterReceiver(timeReceiver);
        }

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");
        setTimeTick(context);
        updateAllWidgets(context, appWidgetManager, appWidgetIds);
    }

    void setTimeTick(Context context) {
        if (timeReceiver != null) return;
        timeReceiver = new TimeReceiver();
        context.getApplicationContext().registerReceiver(
                timeReceiver,
                new IntentFilter(Intent.ACTION_TIME_TICK)
        );
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle bundle) {

        Log.d(TAG, "onAppWidgetOptionsChanged");
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
