package com.firebirdberlin.nightdream.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class DrawingTestView extends View {

    private static final String TAG = "DrawingView";

    private Paint paint = new Paint();

    public DrawingTestView(Context context) {
        super(context);

    }

    public DrawingTestView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onDraw(Canvas canvas) {
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        //paint.setStrokeWidth(2);

        canvas.drawRect(0, 0, getWidth() - 1, getHeight() - 1, paint);
        canvas.drawLine(0, 0, getWidth() - 1, getHeight() - 1, paint);
        canvas.drawLine(0, getHeight() - 1, getWidth() - 1, 0, paint);
    }
}
