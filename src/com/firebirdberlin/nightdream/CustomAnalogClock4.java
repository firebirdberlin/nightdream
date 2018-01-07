package com.firebirdberlin.nightdream;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;

import java.util.Calendar;


public class CustomAnalogClock4 extends CustomAnalogClock {
    final String[] ROMAN_DIGITS = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};
    float centerX = 0.f;
    float centerY = 0.f;
    int radius = 0;

    Decoration decoration = Decoration.NONE;
    float digitPosition = 0.85f;
    DigitStyle digitStyle = DigitStyle.ARABIC;
    boolean emphasizeHour12 = true;
    HandShape handShape = HandShape.TRIANGLE;
    float handLengthHours = 0.8f;
    float handLengthMinutes = 0.95f;
    float handWidthHours = 0.04f;
    float handWidthMinutes = 0.04f;
    boolean highlightQuarterOfHour = true;
    float tickStartMinutes = 0.95f;
    TickStyle tickStyleMinutes = TickStyle.DASH;
    float tickLengthMinutes = 0.04f;
    float tickStartHours = 0.95f;
    TickStyle tickStyleHours = TickStyle.CIRCLE;
    float tickLengthHours = 0.04f;

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

    public void setStyle(Style style) {
        switch (style) {
            case DEFAULT:
                decoration = Decoration.NONE;
                digitPosition = 0.85f;
                digitStyle = DigitStyle.ARABIC;
                emphasizeHour12 = true;
                handShape = HandShape.TRIANGLE;
                handLengthHours = 0.8f;
                handLengthMinutes = 0.95f;
                handWidthHours = 0.04f;
                handWidthMinutes = 0.04f;
                highlightQuarterOfHour = true;
                tickStartMinutes = 0.95f;
                tickStyleMinutes = TickStyle.DASH;
                tickLengthMinutes = 0.04f;
                tickStartHours = 0.95f;
                tickStyleHours = TickStyle.CIRCLE;
                tickLengthHours = 0.04f;
                break;
            case SIMPLE:
                decoration = Decoration.MINUTE_HAND;
                digitPosition = 0.85f;
                digitStyle = DigitStyle.NONE;
                emphasizeHour12 = false;
                handShape = HandShape.TRIANGLE;
                handLengthHours = 0.6f;
                handLengthMinutes = 0.9f;
                handWidthHours = 0.04f;
                handWidthMinutes = 0.04f;
                highlightQuarterOfHour = false;
                tickStartMinutes = 0.87f;
                tickStyleMinutes = TickStyle.NONE;
                tickLengthMinutes = 0.06f;
                tickStartHours = 0.87f;
                tickStyleHours = TickStyle.DASH;
                tickLengthHours = 0.06f;
                break;
            case ARC:
                decoration = Decoration.NONE;
                digitPosition = 0.85f;
                digitStyle = DigitStyle.NONE;
                emphasizeHour12 = false;
                handShape = HandShape.ARC;
                handLengthHours = 0.80f;
                handLengthMinutes = 0.90f;
                handWidthHours = 0.06f;
                handWidthMinutes = 0.06f;
                highlightQuarterOfHour = false;
                tickStartMinutes = 0.87f;
                tickStyleMinutes = TickStyle.NONE;
                tickLengthMinutes = 0.06f;
                tickStartHours = 0.87f;
                tickStyleHours = TickStyle.DASH;
                tickLengthHours = 0.06f;
                break;
        }

    }

    public void onDraw(Canvas canvas) {
        centerX = getWidth() / 2;
        centerY = getHeight() / 2;
        radius = getWidth() / 2 - 20;

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
        paint.setColor(Color.WHITE);

        drawBackgroundArc(canvas, centerX, centerY, radius, min_angle);

        applyShader(paint, centerX, centerY, radius);
        drawTicks(canvas, centerX, centerY, radius);
        drawHourDigits(canvas, centerX, centerY, radius);
        drawHands(canvas, centerX, centerY, radius, hour_angle, min_angle);
    }

    private void drawHands(Canvas canvas, float centerX, float centerY, int radius,
                           double hour_angle, double min_angle) {
        paint.setColorFilter(customColorFilter);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        // minute hand
        canvas.save();
        paint.setColorFilter(customColorFilter);
        canvas.rotate((float) radiansToDegrees(min_angle), centerX, centerY);
        drawHand(canvas, paint, centerX, centerY, (int) (handLengthMinutes * radius),
                (int) (handWidthMinutes * radius));
        canvas.restore();

        // hour hand
        canvas.save();
        paint.setColorFilter(secondaryColorFilter);
        canvas.rotate((float) radiansToDegrees(hour_angle), centerX, centerY);
        drawHand(canvas, paint, centerX, centerY, (int) (handLengthHours * radius),
                (int) (handWidthHours * radius));
        canvas.restore();

        drawScrew(canvas);
    }

    private void drawHand(Canvas canvas, Paint paint, float baseX, float baseY, int height, int width) {
        switch (handShape) {
            case ARC:
                drawHandArc(canvas, height, width);
                break;
            case BAR:
                drawHandBar(canvas, paint, baseX, baseY, height, width);
                break;
            case TRIANGLE:
            default:
                drawHandTriangle(canvas, paint, baseX, baseY, height, width);
                break;
        }
    }

    private void drawHandTriangle(Canvas canvas, Paint paint, float centerX, float centerY, int length, int width) {
        int halfWidth = width / 2;

        Path path = new Path();
        path.moveTo(centerX, centerY - halfWidth);
        path.lineTo(centerX + length, centerY);
        path.lineTo(centerX, centerY + halfWidth);
        path.lineTo(centerX, centerY - halfWidth);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawHandBar(Canvas canvas, Paint paint, float centerX, float centerY, int length, int width) {
        paint.setStrokeWidth(width);
        canvas.drawLine(centerX, centerY, centerX + length, centerY, paint);
    }

    private void drawHandArc(Canvas canvas, int length, int width) {
        canvas.save();
        float px = (float) (.93 - .87) * radius;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        final int[] colors = {Color.TRANSPARENT, Color.WHITE};
        final float[] positions = {0.2f, 1.f};
        Shader gradient = new SweepGradient(centerX, centerY, colors, positions);
        paint.setShader(gradient);
        canvas.drawCircle(centerX, centerY, length, paint);
        paint.setShader(null);
        canvas.restore();
    }

    private void drawScrew(Canvas canvas) {
        if (handShape == HandShape.ARC) return;

        paint.setColorFilter(secondaryColorFilter);
        paint.setAlpha(255);
        canvas.drawCircle(centerX, centerY, 0.045f * radius, paint);
        paint.setColorFilter(null);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2.f);
        canvas.drawPoint(centerX, centerY, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, 0.045f * radius, paint);
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

    private void drawBackgroundArc(Canvas canvas, float centerX, float centerY, int radius, double angle) {
        if (decoration != Decoration.MINUTE_HAND) return;
        canvas.save();
        paint.setAlpha(70);
        paint.setColorFilter(customColorFilter);
        final int[] colors = {Color.TRANSPARENT, Color.WHITE};
        final float[] positions = {0.5f, 1.f};
        Shader gradient = new SweepGradient(centerX, centerY, colors, positions);
        float rotate = (float) radiansToDegrees(angle);
        Matrix gradientMatrix = new Matrix();
        gradientMatrix.preRotate(rotate, centerX, centerY);
        gradient.setLocalMatrix(gradientMatrix);

        paint.setShader(gradient);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(centerX, centerY, handLengthMinutes * radius, paint);
        paint.setShader(null);
        canvas.restore();
    }

    private void applyShader(Paint paint, float centerX, float centerY, int radius) {
        if (decoration != Decoration.LABELS) return;

        int x1 = (int) (centerX - radius), y1 = (int) (centerY - radius);

        float[] hsv = new float[3];
        Color.colorToHSV(customColor, hsv);
        hsv[2] *= 1.2f;
        hsv[1] *= .90f;
        int accent = Color.HSVToColor(hsv);

        int colors[] = {Color.WHITE, accent};
        float positions[] = {0.4f, 1.f};
        Shader shader = new LinearGradient(x1, y1, centerX, centerY, colors, positions, Shader.TileMode.MIRROR);
        paint.setShader(shader);
    }

    private void drawTicks(Canvas canvas, float centerX, float centerY, int radius) {
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

            boolean isHourTick = (minuteCounter % 5 == 0);
            TickStyle tickStyle = (isHourTick) ? tickStyleHours : tickStyleMinutes;
            float tickStart = (isHourTick) ? tickStartHours : tickStartMinutes;
            float tickLength = (isHourTick) ? tickLengthHours : tickLengthMinutes;

            float tickStartX = (float) (centerX + tickStart * radius * Math.cos(angle));
            float tickStartY = (float) (centerY + tickStart * radius * Math.sin(angle));
            float tickEndX = (float) (centerX + (tickStart + tickLength) * radius * Math.cos(angle));
            float tickEndY = (float) (centerY + (tickStart + tickLength) * radius * Math.sin(angle));
            switch (tickStyle) {
                case NONE:
                    break;
                case CIRCLE:
                    if (isHourTick && emphasizeHour12 && minuteCounter == 45) {
                        // for "12" digit draw a special marker
                        float triangleHeight = tickLength * radius * 1.2f;
                        float triangleWidth = triangleHeight * 1.2f;
                        drawTriangle(canvas, paint, tickEndX, tickEndY - triangleHeight * .1f, triangleWidth, triangleHeight);
                    } else {
                        float roundTickRadius = tickLength * .5f * radius;
                        float roundTickCenterX = (centerX + (tickStart + tickLength * .5f) * (float) radius * (float) Math.cos(angle));
                        float roundTickCenterY = (centerY + (tickStart + tickLength * .5f) * (float) radius * (float) Math.sin(angle));
                        canvas.drawCircle(roundTickCenterX, roundTickCenterY, roundTickRadius, paint);
                    }
                    break;
                default:
                    canvas.drawLine(tickStartX, tickStartY, tickEndX, tickEndY, paint);
                    break;
            }
            minuteCounter++;
        }
    }

    private void drawHourDigits(Canvas canvas, float centerX, float centerY, int radius) {
        if (digitStyle == DigitStyle.NONE) return;
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
                    paint.setTypeface(Typeface.create(typeface, Typeface.BOLD));
                } else {
                    paint.setColorFilter(secondaryColorFilter);
                    paint.setTextSize(digitFontSizeSmall);
                    paint.setTypeface(Typeface.create(typeface, Typeface.NORMAL));
                }
            }

            float x = (float) (centerX + digitPosition * radius * Math.cos(angle));
            float y = (float) (centerY + digitPosition * radius * Math.sin(angle));

            Rect bounds = new Rect();
            String currentHourText = (digitStyle == DigitStyle.ARABIC)
                                        ? String.valueOf(currentHour)
                                        : ROMAN_DIGITS[currentHour - 1];

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

    public enum DigitStyle {NONE, ARABIC, ROMAN}

    public enum HandShape {TRIANGLE, BAR, ARC}

    public enum TickStyle {NONE, DASH, CIRCLE}

    public enum Decoration {NONE, MINUTE_HAND, LABELS}
}
