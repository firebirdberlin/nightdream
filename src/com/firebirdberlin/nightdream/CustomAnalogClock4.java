package com.firebirdberlin.nightdream;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;

import java.util.Calendar;


public class CustomAnalogClock4 extends CustomAnalogClock {
    private static final String TAG = "CustomAnalogClock";

    public CustomAnalogClock4(Context context) {
        super(context);
    }

    public CustomAnalogClock4(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onDraw(Canvas canvas) {
        float centerX = getWidth() / 2;
        float centerY = getHeight() / 2;
        int radius = getWidth() / 2 - 20;

        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);

        int hour = Calendar.getInstance().get(Calendar.HOUR);
        int min = Calendar.getInstance().get(Calendar.MINUTE);

        // radian unit (rad), 2PI = 360 degree = 12h -> 3:00/15:00 = 0 rad
        // 12h/6=2PI
        // added minute offset: 60min = 2PI/12h -> 60min = PI/6h
        double hour_angle = (double) hour / 6. * Math.PI - Math.PI / 2.
                + (double) min / 60. * Math.PI / 6.;
        // 60min = 2PI -> min / 60 * 2PI
        double min_angle = (double) min / 30. * Math.PI - Math.PI / 2.;

        paint.setAlpha(255);
        paint.setColorFilter(customColorFilter);
        paint.setStyle(Paint.Style.FILL);

        float tickStart = .95f;
        float tickEnd = .99f;
        float hourDigitStart = .85f;

        drawHands(canvas, centerX, centerY, radius, hour_angle, min_angle, tickStart, tickEnd, hourDigitStart - .05f);

        paint.setColor(Color.WHITE);

        drawTicks(canvas, centerX, centerY, radius, tickStart, tickEnd);

        drawHourDigits(canvas, centerX, centerY, radius, hourDigitStart);


    }

    private void drawHands(Canvas canvas, float centerX, float centerY, int radius, double hour_angle, double min_angle, float tickStart, float tickEnd, float hourDigitStart) {
        // minute hand
        canvas.save();
        paint.setColorFilter(customColorFilter);
        canvas.rotate((float) radiansToDegrees(min_angle), centerX, centerY);
        drawHand(canvas, paint, centerX, centerY, (int) (tickStart * radius), (int) (0.04f * radius));
        canvas.restore();

        // hour hand
        canvas.save();
        paint.setColorFilter(secondaryColorFilter);
        canvas.rotate((float) radiansToDegrees(hour_angle), centerX, centerY);
        drawHand(canvas, paint, centerX, centerY, (int) (hourDigitStart * radius), (int) (0.04f * radius));
        canvas.restore();

        paint.setColorFilter(secondaryColorFilter);
        paint.setAlpha(255);
        canvas.drawCircle(centerX, centerY, 0.045f * radius, paint);

        paint.setColorFilter(null);
        paint.setColor(Color.BLACK);
        canvas.drawPoint(centerX, centerY, paint);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(centerX, centerY, 0.045f * radius, paint);
    }

    private void drawHand(Canvas canvas, Paint paint, float baseX, float baseY, int height, int width) {
        int halfWidth = width / 2;

        Path path = new Path();
        path.moveTo(baseX, baseY - halfWidth);
        path.lineTo(baseX + height, baseY);
        path.lineTo(baseX, baseY + halfWidth);
        path.lineTo(baseX, baseY - halfWidth);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawTicks(Canvas canvas, float centerX, float centerY, int radius, float tickStart, float tickEnd) {
        // ticks
        paint.setShader(null);
        paint.setAlpha(150);
        paint.setColorFilter(secondaryColorFilter);

        int width = Utility.dpToPx(context, 1.f);
        paint.setStrokeWidth(width);
        paint.setStyle(Paint.Style.FILL); //filled circle for every fifth minute
        int minuteCounter = 0;

        for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 30) {

            boolean roundTick = (minuteCounter % 5 == 0);

            //tickStart = (roundTick ? .94f : .95f);

            float tickStartX = (int) (centerX + tickStart * radius * Math.cos(angle));
            float tickStartY = (int) (centerY + tickStart * radius * Math.sin(angle));
            float tickEndX = (int) (centerX + tickEnd * radius * Math.cos(angle));
            float tickEndY = (int) (centerY + tickEnd * radius * Math.sin(angle));

            if (roundTick) {
                float roundTickRadius = (tickEnd - tickStart) * .5f * radius;
                float roundTickCenterX = (int) (centerX + (tickStart + (tickEnd - tickStart) * .5f) * radius * Math.cos(angle));
                float roundTickCenterY = (int) (centerY + (tickStart + (tickEnd - tickStart) * .5f) * radius * Math.sin(angle));
                canvas.drawCircle(roundTickCenterX, roundTickCenterY, roundTickRadius, paint);
            } else {
                canvas.drawLine(tickStartX, tickStartY, tickEndX, tickEndY, paint);
            }
            minuteCounter++;
        }
    }

    private void drawHourDigits(Canvas canvas, float centerX, float centerY, int radius, float hourDigitStart) {
        paint.setTextSize(30);
        paint.setStrokeWidth(0);

        int digitCounter = 0;
        boolean highlightQuarterOfHour = true;
        for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 6) {

            int currentHour = (digitCounter + 2) % 12 + 1;

            if (highlightQuarterOfHour) {
                if (currentHour % 3 == 0) {
                    // 3,6,9,12
                    paint.setColorFilter(customColorFilter);
                    paint.setTextSize(31);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                } else {
                    paint.setColorFilter(secondaryColorFilter);
                    paint.setTextSize(28);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                }
            }

            float x = (float) (centerX + hourDigitStart * radius * Math.cos(angle));
            float y = (float) (centerY + hourDigitStart * radius * Math.sin(angle));

            Rect bounds = new Rect();
            String currentHourText = String.valueOf(currentHour);
            paint.getTextBounds(currentHourText, 0, currentHourText.length(), bounds);

            // for width measureText returns more exact result than textbounds
            // for height textbounds is ok
            float textWidth = paint.measureText(currentHourText, 0, currentHourText.length());

            x -= textWidth / 2.;
            y -= bounds.height() / 2 + 1f;

            canvas.drawText(currentHourText, x, y + bounds.height(), paint);
            digitCounter++;

            //debug test alignment:
            // ray
            //float rayEndX = (float) (center.x + .93 * radius * Math.cos(angle));
            //float rayEndY = (float) (center.y + .93 * radius * Math.sin(angle));
            //canvas.drawLine(center.x, center.y, rayEndX, rayEndY, paint);
            // text bounding box
            //canvas.drawRect(x, y, x + textWidth, y + bounds.height(), paint);

        }
    }

    private void drawArc(Canvas canvas, int radius, double angle, float centerX, float centerY) {
        final int[] colors = {Color.TRANSPARENT, Color.WHITE};
        final float[] positions = {0.2f, 1.f};
        Shader gradient = new SweepGradient(centerX, centerY, colors, positions);
        float rotate = (float) radiansToDegrees(angle);
        Matrix gradientMatrix = new Matrix();
        gradientMatrix.preRotate(rotate, centerX, centerY);
        gradient.setLocalMatrix(gradientMatrix);


        paint.setShader(gradient);
        canvas.drawCircle(centerX, centerY, 0.9f * radius, paint);
        paint.setShader(null);
    }

    private double radiansToDegrees(double rad) {
        return rad * 180. / Math.PI;
    }
}
