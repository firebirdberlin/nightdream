package com.firebirdberlin.nightdream.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import com.firebirdberlin.nightdream.services.ScreenWatcherService;
import com.firebirdberlin.nightdream.services.WeatherService;
import com.firebirdberlin.nightdream.ui.ClockLayout;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ClockWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "WidgetProvider";
    private static final String LOG_FILE_WEATHER_UPDATE = "nightdream_weather_update_log.txt";

    private static ViewInfo prepareSourceView(Context context, WidgetDimension dimension) {

        final Dimension widgetSize = actualWidgetSize(context, dimension);

        // convert width/height from dip to pixels, otherwise widgetBitmap is blurry
        int widthPixel = Utility.dpToPx(context, widgetSize.width);
        int heightPixel = Utility.dpToPx(context, widgetSize.height);

        // load a view from resource
        LayoutInflater inflater = LayoutInflater.from(context);
        View container = inflater.inflate(R.layout.clock_widget_clock_layout, null);

        ClockLayout clockLayout = container.findViewById(R.id.clockLayout);

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
        int clockLayoutId = settings.getClockLayoutID(false);
        int glowRadius = settings.getGlowRadius(clockLayoutId);
        int textureId = settings.getTextureResId(clockLayoutId);


        clockLayout.setBackgroundColor(Color.TRANSPARENT);
        clockLayout.setLayout(clockLayoutId);
        clockLayout.setTypeface(settings.typeface);
        clockLayout.setPrimaryColor(settings.clockColor, glowRadius, settings.clockColor, textureId);
        clockLayout.setSecondaryColor(settings.secondaryColor);
        clockLayout.setDateFormat(settings.dateFormat);
        clockLayout.setTimeFormat(settings.getTimeFormat(), settings.is24HourFormat());
        clockLayout.setShowDivider(settings.showDivider);
        clockLayout.showDate(showAdditionalLines && settings.showDate);
        clockLayout.setShowNotifications(false);

        WeatherService.start(context, settings.weatherCityID);

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
        // called when first widget instance is put to home screen
        super.onEnabled(context);
        Log.i(TAG, "onEnabled");
        ScreenWatcherService.conditionallyStart(context);
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

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle bundle) {
        WidgetDimension w = widgetDimensionFromBundle(bundle);
        Log.d(TAG, String.format("onUpdate: widgetId=%d minwidth=%d maxwidth=%d minheight=%d maxheight=%d", appWidgetId, w.minWidth, w.maxWidth, w.minHeight, w.maxHeight));

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
}
