/*
 * NightDream
 * Copyright (C) 2026 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeatherGraphView extends View {

    private static final String TAG = WeatherGraphView.class.getSimpleName();

    private static final String DEFAULT_TIME_FORMAT = "HH:mm";
    private static final int COLOR_RAIN = Color.argb(180, 100, 180, 255);
    private static final int COLOR_RAIN_TEXT = Color.rgb(100, 180, 255);
    private static final int COLOR_TEXT = Color.argb(200, 255, 255, 255);
    private static final int COLOR_GRID = Color.argb(60, 255, 255, 255);
    private static final int COLOR_NIGHT = Color.argb(40, 0, 0, 0);
    private final List<WeatherEntry> entries = new ArrayList<>();
    private String[] timeLabels = new String[0];

    private final Paint linePaint = new Paint();
    private final Paint apparentLinePaint = new Paint();
    private final Paint barPaint = new Paint();
    private final Paint gridPaint = new Paint();
    private final Paint axisPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint rainTextPaint = new Paint();
    private final Paint nightPaint = new Paint();
    private final Path linePath = new Path();
    private final Path apparentLinePath = new Path();

    private boolean showApparentTemperature = false;
    private int temperatureUnit = WeatherEntry.CELSIUS;
    private SimpleDateFormat timeFormat;
    private String timeFormatPattern = DEFAULT_TIME_FORMAT;

    public WeatherGraphView(@Nullable Context context) {
        super(context);
        init();
    }

    public WeatherGraphView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private float dp(float value) {
        if (getResources() == null || getResources().getDisplayMetrics() == null) {
            return value;
        }
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        if (getResources() == null || getResources().getDisplayMetrics() == null) {
            return value;
        }
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }

    private void init() {
        linePaint.setAntiAlias(true);
        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(3f);
        linePaint.setStyle(Paint.Style.STROKE);

        apparentLinePaint.setAntiAlias(true);
        apparentLinePaint.setColor(Color.argb(180, 255, 255, 255));
        apparentLinePaint.setStrokeWidth(2f);
        apparentLinePaint.setStyle(Paint.Style.STROKE);
        apparentLinePaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{8f, 8f}, 0f));

        barPaint.setAntiAlias(true);
        barPaint.setColor(COLOR_RAIN);
        barPaint.setStyle(Paint.Style.FILL);

        gridPaint.setAntiAlias(true);
        gridPaint.setColor(COLOR_GRID);
        gridPaint.setStrokeWidth(Math.max(1f, dp(0.5f)));
        gridPaint.setStyle(Paint.Style.STROKE);

        axisPaint.setAntiAlias(true);
        axisPaint.setColor(COLOR_TEXT);
        axisPaint.setStrokeWidth(Math.max(1f, dp(1f)));
        axisPaint.setStyle(Paint.Style.STROKE);

        textPaint.setAntiAlias(true);
        textPaint.setColor(COLOR_TEXT);
        textPaint.setTextSize(sp(10f));

        rainTextPaint.setAntiAlias(true);
        rainTextPaint.setColor(COLOR_RAIN_TEXT);
        rainTextPaint.setTextSize(sp(10f));

        nightPaint.setStyle(Paint.Style.FILL);
        nightPaint.setColor(COLOR_NIGHT);

        timeFormat = new SimpleDateFormat(timeFormatPattern, Locale.getDefault());
    }

    /**
     * Sets the pattern for the x-axis labels. Use the same pattern as
     * WeatherForecastLayout#setTimeFormat (settings.getFullTimeFormat()).
     */
    public void setTimeFormat(@Nullable String pattern) {
        String newPattern = (pattern != null && !pattern.isEmpty()) ? pattern : DEFAULT_TIME_FORMAT;
        if (newPattern.equals(timeFormatPattern)) {
            return;
        }
        try {
            timeFormat = new SimpleDateFormat(newPattern, Locale.getDefault());
            timeFormatPattern = newPattern;
        } catch (IllegalArgumentException e) {
            timeFormat = new SimpleDateFormat(DEFAULT_TIME_FORMAT, Locale.getDefault());
            timeFormatPattern = DEFAULT_TIME_FORMAT;
        }
        updateTimeLabels();
        invalidate();
    }

    private void updateTimeLabels() {
        timeLabels = new String[entries.size()];
        Date date = new Date();
        for (int i = 0; i < entries.size(); i++) {
            date.setTime(entries.get(i).timestamp * 1000L);
            timeLabels[i] = timeFormat.format(date);
        }
    }

    public void setEntries(@Nullable List<WeatherEntry> weatherEntries, int temperatureUnit,
                           boolean showApparentTemperature) {
        this.entries.clear();
        if (weatherEntries != null) {
            this.entries.addAll(weatherEntries);
            this.entries.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
        }
        this.temperatureUnit = temperatureUnit;
        this.showApparentTemperature = showApparentTemperature;

        updateTimeLabels();
        invalidate();
    }

    /**
     * WeatherEntry.temperature is stored in Kelvin.
     */
    double toDisplayTemperature(double kelvin) {
        return switch (temperatureUnit) {
            case WeatherEntry.CELSIUS -> kelvin - 273.15;
            case WeatherEntry.FAHRENHEIT -> kelvin * 1.8 - 459.67;
            default -> kelvin;
        };
    }

    static double rainOf(WeatherEntry entry) {
        return entry.rain3h > 0 ? entry.rain3h : (entry.rain1h > 0 ? entry.rain1h : 0.0);
    }

    String formatTemperature(double value) {
        return String.format(Locale.getDefault(), "%d°", Math.round(value));
    }

    static void drawTextVCentered(Canvas canvas, String text, float x, float yCenter,
                                  Paint paint) {
        float baseline = yCenter - (paint.ascent() + paint.descent()) / 2f;
        canvas.drawText(text, x, baseline, paint);
    }

    final DataRanges dataRanges = new DataRanges();

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (entries.isEmpty()) {
            return;
        }

        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();
        if (width <= 0 || height <= 0) {
            return;
        }

        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());

        calculateDataRanges(dataRanges);

        float gap = dp(4f);
        float textH = textPaint.descent() - textPaint.ascent();

        String maxTempLabel = formatTemperature(dataRanges.maxTemp);
        String minTempLabel = formatTemperature(dataRanges.minTemp);
        String midTempLabel = formatTemperature((dataRanges.minTemp + dataRanges.maxTemp) / 2.0);
        String rainLabel = String.format(Locale.getDefault(), "%.1f mm", dataRanges.maxRain);

        float yLabelWidth = Math.max(
                textPaint.measureText(maxTempLabel),
                Math.max(textPaint.measureText(minTempLabel), textPaint.measureText(midTempLabel))
        );
        float maxTimeLabelWidth = getMaxTimeLabelWidth();
        float halfTimeLabel = maxTimeLabelWidth / 2f;

        float leftMargin = Math.max(yLabelWidth + gap, halfTimeLabel);
        float rightMargin = Math.max(
                dataRanges.hasRain ? rainTextPaint.measureText(rainLabel) + gap : 0f,
                halfTimeLabel
        );
        float topMargin = textH / 2f;
        float bottomMargin = textH + gap * 2f;

        float plotLeft = leftMargin;
        float plotRight = width - rightMargin;
        float plotTop = topMargin;
        float plotBottom = height - bottomMargin;
        float plotWidth = plotRight - plotLeft;
        float plotHeight = plotBottom - plotTop;
        if (plotWidth <= 0 || plotHeight <= 0) {
            canvas.restore();
            return;
        }

        int size = entries.size();
        long startTime = entries.get(0).timestamp * 1000L;
        long endTime = entries.get(size - 1).timestamp * 1000L;

        float lineAreaHeight = plotHeight * 0.65f;
        float barAreaHeight = plotHeight * 0.35f;

        drawNightBackground(canvas, startTime, endTime, plotLeft, plotTop, plotBottom, plotWidth);

        drawTemperatureGrid(canvas, plotLeft, plotRight, plotTop, lineAreaHeight, gap, textH,
                maxTempLabel, minTempLabel, midTempLabel);

        drawRainBars(canvas, size, startTime, endTime, plotLeft, plotRight, plotBottom,
                plotWidth, barAreaHeight, dataRanges.maxRain, dataRanges.hasRain, rainLabel, gap);

        drawTemperatureLines(canvas, size, startTime, endTime, plotLeft, plotTop, plotWidth,
                lineAreaHeight, dataRanges.minTemp, dataRanges.maxTemp);

        drawXAxis(canvas, size, startTime, endTime, plotLeft, plotRight, plotBottom, plotWidth,
                gap, textH, maxTimeLabelWidth);

        canvas.restore();
    }

    void calculateDataRanges(DataRanges ranges) {
        ranges.minTemp = Double.MAX_VALUE;
        ranges.maxTemp = -Double.MAX_VALUE;
        ranges.maxRain = 0.0;

        for (WeatherEntry entry : entries) {
            double t = toDisplayTemperature(entry.temperature);
            if (t < ranges.minTemp) {
                ranges.minTemp = t;
            }
            if (t > ranges.maxTemp) {
                ranges.maxTemp = t;
            }
            if (showApparentTemperature && entry.apparentTemperature > 0.f) {
                double at = toDisplayTemperature(entry.apparentTemperature);
                if (at < ranges.minTemp) {
                    ranges.minTemp = at;
                }
                if (at > ranges.maxTemp) {
                    ranges.maxTemp = at;
                }
            }
            double rain = rainOf(entry);
            if (rain > ranges.maxRain) {
                ranges.maxRain = rain;
            }
        }

        ranges.hasRain = ranges.maxRain > 0.0;
        if (ranges.minTemp == ranges.maxTemp) {
            ranges.minTemp -= 1.0;
            ranges.maxTemp += 1.0;
        }
        if (!ranges.hasRain) {
            ranges.maxRain = 1.0;
        }
    }

    float getMaxTimeLabelWidth() {
        float maxTimeLabelWidth = 0f;
        for (String label : timeLabels) {
            maxTimeLabelWidth = Math.max(maxTimeLabelWidth, textPaint.measureText(label));
        }
        return maxTimeLabelWidth;
    }

    void drawNightBackground(Canvas canvas, long startTime, long endTime,
                             float plotLeft, float plotTop, float plotBottom,
                             float plotWidth) {
        if (entries.size() <= 1) {
            return;
        }
        float lat = 0.0f;
        float lon = 0.0f;
        for (WeatherEntry entry : entries) {
            if (entry.lat != 0.0f || entry.lon != 0.0f) {
                lat = entry.lat;
                lon = entry.lon;
                break;
            }
        }
        List<Utility.NightInterval> nightIntervals = Utility.calculateNightIntervals(
                startTime, endTime, lat, lon);
        for (Utility.NightInterval interval : nightIntervals) {
            long nightStart = Math.max(interval.start, startTime);
            long nightEnd = Math.min(interval.end, endTime);
            if (nightEnd > nightStart) {
                float left = timeToX(nightStart, startTime, endTime, plotLeft, plotWidth);
                float right = timeToX(nightEnd, startTime, endTime, plotLeft, plotWidth);
                canvas.drawRect(left, plotTop, right, plotBottom, nightPaint);
            }
        }
    }

    void drawTemperatureGrid(Canvas canvas, float plotLeft, float plotRight,
                             float plotTop, float lineAreaHeight, float gap, float textH,
                             String maxTempLabel, String minTempLabel,
                             String midTempLabel) {
        float yMax = plotTop;
        float yMin = plotTop + lineAreaHeight;
        float yMid = (yMax + yMin) / 2f;

        canvas.drawLine(plotLeft, yMax, plotRight, yMax, gridPaint);
        canvas.drawLine(plotLeft, yMin, plotRight, yMin, gridPaint);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        drawTextVCentered(canvas, maxTempLabel, plotLeft - gap, yMax, textPaint);
        drawTextVCentered(canvas, minTempLabel, plotLeft - gap, yMin, textPaint);
        if ((yMin - yMax) / 2f > textH * 1.2f) {
            canvas.drawLine(plotLeft, yMid, plotRight, yMid, gridPaint);
            drawTextVCentered(canvas, midTempLabel, plotLeft - gap, yMid, textPaint);
        }
    }

    void drawRainBars(Canvas canvas, int size, long startTime, long endTime,
                      float plotLeft, float plotRight, float plotBottom,
                      float plotWidth, float barAreaHeight, double maxRain,
                      boolean hasRain, String rainLabel, float gap) {
        for (int i = 0; i < size; i++) {
            double rain = rainOf(entries.get(i));
            if (rain > 0) {
                float x = timeToX(entries.get(i).timestamp * 1000L, startTime, endTime,
                        plotLeft, plotWidth);
                float deltaLeft;
                float deltaRight;
                if (size == 1) {
                    deltaLeft = plotWidth * 0.5f;
                    deltaRight = plotWidth * 0.5f;
                } else {
                    if (i > 0) {
                        float prevX = timeToX(entries.get(i - 1).timestamp * 1000L,
                                startTime, endTime, plotLeft, plotWidth);
                        deltaLeft = x - prevX;
                    } else {
                        float nextX = timeToX(entries.get(1).timestamp * 1000L,
                                startTime, endTime, plotLeft, plotWidth);
                        deltaLeft = nextX - x;
                    }

                    if (i < size - 1) {
                        float nextX = timeToX(entries.get(i + 1).timestamp * 1000L,
                                startTime, endTime, plotLeft, plotWidth);
                        deltaRight = nextX - x;
                    } else {
                        float prevX = timeToX(entries.get(size - 2).timestamp * 1000L,
                                startTime, endTime, plotLeft, plotWidth);
                        deltaRight = x - prevX;
                    }
                }

                float barHeight = (float) (rain / maxRain) * barAreaHeight;
                float left = x - deltaLeft * 0.25f;
                float right = x + deltaRight * 0.25f;
                canvas.drawRect(Math.max(plotLeft, left), plotBottom - barHeight,
                        Math.min(plotRight, right), plotBottom, barPaint);
            }
        }
        if (hasRain) {
            float yRain = plotBottom - barAreaHeight;
            rainTextPaint.setTextAlign(Paint.Align.LEFT);
            drawTextVCentered(canvas, rainLabel, plotRight + gap, yRain, rainTextPaint);
        }
    }

    void drawTemperatureLines(Canvas canvas, int size, long startTime, long endTime,
                              float plotLeft, float plotTop, float plotWidth,
                              float lineAreaHeight, double minTemp, double maxTemp) {
        linePath.reset();
        for (int i = 0; i < size; i++) {
            double t = toDisplayTemperature(entries.get(i).temperature);
            float x = timeToX(entries.get(i).timestamp * 1000L, startTime, endTime,
                    plotLeft, plotWidth);
            float ratio = (float) ((t - minTemp) / (maxTemp - minTemp));
            float y = plotTop + lineAreaHeight * (1.0f - ratio);
            if (i == 0) {
                linePath.moveTo(x, y);
            } else {
                linePath.lineTo(x, y);
            }
        }
        canvas.drawPath(linePath, linePaint);

        if (showApparentTemperature) {
            apparentLinePath.reset();
            boolean hasApparent = false;
            for (int i = 0; i < size; i++) {
                WeatherEntry entry = entries.get(i);
                if (entry.apparentTemperature > 0.f) {
                    double at = toDisplayTemperature(entry.apparentTemperature);
                    float x = timeToX(entry.timestamp * 1000L, startTime, endTime,
                            plotLeft, plotWidth);
                    float ratio = (float) ((at - minTemp) / (maxTemp - minTemp));
                    float y = plotTop + lineAreaHeight * (1.0f - ratio);
                    if (!hasApparent) {
                        apparentLinePath.moveTo(x, y);
                        hasApparent = true;
                    } else {
                        apparentLinePath.lineTo(x, y);
                    }
                }
            }
            if (hasApparent) {
                canvas.drawPath(apparentLinePath, apparentLinePaint);
            }
        }
    }

    void drawXAxis(Canvas canvas, int size, long startTime, long endTime,
                   float plotLeft, float plotRight, float plotBottom,
                   float plotWidth, float gap, float textH, float maxTimeLabelWidth) {
        canvas.drawLine(plotLeft, plotBottom, plotRight, plotBottom, axisPaint);

        float tickLength = dp(3f);
        float yTimeLabel = plotBottom + gap + textH / 2f;
        float minLabelSpacing = maxTimeLabelWidth + gap * 2f;
        float lastLabelX = -1f;

        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < size && i < timeLabels.length; i++) {
            float x = timeToX(entries.get(i).timestamp * 1000L, startTime, endTime,
                    plotLeft, plotWidth);
            canvas.drawLine(x, plotBottom, x, plotBottom + tickLength, axisPaint);
            if (lastLabelX < 0 || x - lastLabelX >= minLabelSpacing) {
                drawTextVCentered(canvas, timeLabels[i], x, yTimeLabel, textPaint);
                lastLabelX = x;
            }
        }
    }

    static float timeToX(long timeMs, long startTime, long endTime,
                         float plotLeft, float plotWidth) {
        if (endTime == startTime) {
            return plotLeft;
        }
        return plotLeft + (float) ((double) (timeMs - startTime) / (endTime - startTime) * plotWidth);
    }

    static class DataRanges {
        double minTemp;
        double maxTemp;
        double maxRain;
        boolean hasRain;
    }
}
