package com.firebirdberlin.nightdream;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;

import java.util.Calendar;


public class CustomAnalogClock4 extends CustomAnalogClock {
    final boolean highlightQuarterOfHour = true;
    final boolean emphasizeHour12 = true;
    final boolean enableShader = false;

    public CustomAnalogClock4(Context context) {
        super(context);
    }

    public CustomAnalogClock4(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private static float fontSizeForWidth(String dummyText, float destWidth, Paint paint) {
        final float dummyFontSize = 48f;
        paint.setTextSize(dummyFontSize);
        Rect bounds = new Rect();
        paint.getTextBounds(dummyText, 0, dummyText.length(), bounds);
        return dummyFontSize * destWidth / bounds.width();
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

        float tickStart = .95f;
        float tickEnd = .99f;
        float hourDigitStart = .85f;

        paint.setAlpha(255);
        paint.setColor(Color.WHITE);

        applyShader(paint, centerX, centerY, radius);

        drawTicks(canvas, centerX, centerY, radius, tickStart, tickEnd);
        drawHourDigits(canvas, centerX, centerY, radius, hourDigitStart);

        paint.setColorFilter(customColorFilter);
        paint.setStyle(Paint.Style.FILL);

        drawHands(canvas, centerX, centerY, radius, hour_angle, min_angle, tickStart,
                hourDigitStart - .05f);
    }

    private void drawHands(Canvas canvas, float centerX, float centerY, int radius,
                           double hour_angle, double min_angle, float tickStart,
                           float hourDigitStart) {
        paint.setShader(null);
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

    private void drawTriangle(Canvas canvas, Paint paint, float baseX, float baseY, float width, float height) {
        float halfWidth = width / 2;

        Path path = new Path();
        path.moveTo(baseX - halfWidth, baseY);
        path.lineTo(baseX + halfWidth, baseY);
        path.lineTo(baseX, baseY + height);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void applyShader(Paint paint, float centerX, float centerY, int radius) {
        if (!enableShader) return;

        int x1 = (int) (centerX - radius), y1 = (int) (centerY - radius);

        float[] hsv = new float[3];
        Color.colorToHSV(customColor, hsv);
        hsv[2] *= 1.2f;
        hsv[1] *= .50f;
        int accent = Color.HSVToColor(hsv);

        int colors[] = {Color.WHITE, accent};
        float positions[] = {0.4f, 1.f};
        Shader shader = new LinearGradient(x1, y1, centerX, centerY, colors, positions, Shader.TileMode.MIRROR);
        paint.setShader(shader);
    }

    private void drawTicks(Canvas canvas, float centerX, float centerY, int radius, float tickStart, float tickEnd) {
        // ticks
        paint.setAlpha(255);
        paint.setColorFilter(secondaryColorFilter);

        int width = Utility.dpToPx(context, 1.f);
        paint.setStrokeWidth(width);
        paint.setStyle(Paint.Style.FILL); //filled circle for every hour
        int minuteCounter = 0;

        float angleDelta = (float) Math.PI / 30f;
        float angleMax = (float) (2f * Math.PI);
        for (double angle = 0; angle < angleMax; angle += angleDelta) {

            boolean roundTick = (minuteCounter % 5 == 0);
            float tickStartX = (float) (centerX + tickStart * radius * Math.cos(angle));
            float tickStartY = (float) (centerY + tickStart * radius * Math.sin(angle));
            float tickEndX = (float) (centerX + tickEnd * radius * Math.cos(angle));
            float tickEndY = (float) (centerY + tickEnd * radius * Math.sin(angle));


            if (emphasizeHour12 && minuteCounter == 45) {
                // for "12" digit draw a special marker
                float triangleHeight = (tickEnd - tickStart) * radius * 1.2f;
                float triangleWidth = triangleHeight * 1.2f;
                drawTriangle(canvas, paint, tickEndX, tickEndY - triangleHeight * .1f, triangleWidth, triangleHeight);
            } else if (roundTick) {
                float roundTickRadius = (tickEnd - tickStart) * .5f * radius;
                float roundTickCenterX =  (centerX + (tickStart + (tickEnd - tickStart) * .5f) * (float) radius * (float) Math.cos(angle));
                float roundTickCenterY =  (centerY + (tickStart + (tickEnd - tickStart) * .5f) * (float) radius * (float) Math.sin(angle));
                canvas.drawCircle(roundTickCenterX, roundTickCenterY, roundTickRadius, paint);
            } else {
                canvas.drawLine(tickStartX, tickStartY, tickEndX, tickEndY, paint);
            }
            minuteCounter++;
        }
    }

    private void drawHourDigits(Canvas canvas, float centerX, float centerY, int radius, float hourDigitStart) {
        // calculate font-size for desired text width, so digits have equal size on any device
        final float digitFontSizeBig = fontSizeForWidth("5", 0.08f * radius, paint);
        final float digitFontSizeSmall = fontSizeForWidth("5", 0.06f * radius, paint);
        paint.setTextSize(digitFontSizeBig);

        paint.setStrokeWidth(0);

        int digitCounter = 0;
        for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 6) {

            int currentHour = (digitCounter + 2) % 12 + 1;

            if (highlightQuarterOfHour) {
                if (currentHour % 3 == 0) {
                    // 3,6,9,12
                    paint.setColorFilter(customColorFilter);
                    paint.setTextSize(digitFontSizeBig);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                } else {
                    paint.setColorFilter(secondaryColorFilter);
                    paint.setTextSize(digitFontSizeSmall);
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
            float textHeight = bounds.height();

            // move center of text bounding box to x/y
            x -= textWidth / 2.;
            y -= textHeight / 2f + 1f;

            canvas.drawText(currentHourText, x, y + textHeight, paint);
            digitCounter++;
        }
    }

    private double radiansToDegrees(double rad) {
        return rad * 180. / Math.PI;
    }
}
