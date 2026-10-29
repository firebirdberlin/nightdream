/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
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
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;


public class DirectionIconView extends View {
    public static final float INVALID = -1.f;
    public static final int MODE_HEADING = 0;
    public static final int MODE_ORIGIN = 1;
    private static final String TAG = DirectionIconView.class.getSimpleName();

    Context context;
    Paint paint = new Paint();
    int customcolor = Color.WHITE;
    int width;
    int height;
    float direction = INVALID;
    int mode = MODE_HEADING;
    ColorFilter colorFilter;

    public DirectionIconView(Context context) {
        super(context);
        init(context);
    }

    public DirectionIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        colorFilter = new LightingColorFilter(customcolor, 1);
    }

    public void setColor(int color) {
        customcolor = color;
        colorFilter = new LightingColorFilter(customcolor, 1);
        invalidate();
    }

    public void setMode(int mode) {
        if (this.mode != mode) {
            this.mode = mode;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int heightWithoutPadding = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

        height = heightWithoutPadding;
        setMeasuredDimension(height + getPaddingLeft() + getPaddingRight(),
                height + getPaddingTop() + getPaddingBottom());
    }

    public void setDirection(float direction) {
        if (this.direction != direction) {
            this.direction = direction;
            invalidate();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (this.direction == INVALID) return;

        int half = height / 2;
        canvas.save();
        canvas.translate((float) getPaddingLeft(), (float) getPaddingBottom());

        paint.setAntiAlias(true);
        paint.setColorFilter(colorFilter);
        paint.setColor(Color.WHITE);
        paint.setAlpha(255);

        if (mode == MODE_ORIGIN) {
            // Classic icon pointing to direction where wind is coming from.
            // Unrotated tip points RIGHT (90 degrees).
            canvas.rotate(this.direction - 90.f, half, half);

            paint.setStrokeWidth(1);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            Point a = new Point(5, (int) (0.7f * height));
            Point b = new Point((int) (.33f * height), half);
            Point c = new Point(height, half);

            Path path = prepTriangle(a, b, c);
            canvas.drawPath(path, paint);

            paint.setStrokeWidth(3);
            paint.setStyle(Paint.Style.STROKE);
            a = new Point((int) (.33f * height), half);
            b = new Point(5, (int) (0.3f * height));
            c = new Point(height, half);

            path = prepTriangle(a, b, c);
            canvas.drawPath(path, paint);
        } else {
            // MODE_HEADING: Simple arrow pointing in direction where wind is heading to.
            // Unrotated tip points UP (0 degrees).
            canvas.rotate(this.direction + 180.f, half, half);

            paint.setStyle(Paint.Style.FILL);
            Path path = drawSimpleArrow(height);
            canvas.drawPath(path, paint);
        }
        canvas.restore();
    }

    private Path drawSimpleArrow(int height) {
        float h = (float) height;
        Path path = new Path();
        path.moveTo(0.50f * h, 0.08f * h);
        path.lineTo(0.82f * h, 0.45f * h);
        path.lineTo(0.62f * h, 0.45f * h);
        path.lineTo(0.62f * h, 0.92f * h);
        path.lineTo(0.38f * h, 0.92f * h);
        path.lineTo(0.38f * h, 0.45f * h);
        path.lineTo(0.18f * h, 0.45f * h);
        path.close();
        return path;
    }

    private Path prepTriangle(Point a, Point b, Point c) {
        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(a.x, a.y);
        path.lineTo(b.x, b.y);
        path.lineTo(c.x, c.y);
        path.lineTo(a.x, a.y);
        path.close();
        return path;
    }
}
