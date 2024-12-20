package com.firebirdberlin.nightdream.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.BatteryValue;
import com.firebirdberlin.nightdream.repositories.BatteryStats;
import com.firebirdberlin.nightdream.viewmodels.BatteryReferenceViewModel;
import com.firebirdberlin.nightdream.viewmodels.RSSViewModel;


public class BatteryIconView extends View {
    private static final String TAG = "BatteryIconView";
    private static final float VALUE_FULLY_CHARGED = 95.f;

    Context context;
    Settings settings;
    Paint paint = new Paint();
    int customColor = Color.WHITE;
    float batteryTextOffsetX;
    float batteryIconHeight;
    float batteryIconWidth;
    ColorFilter colorFilter;
    BatteryValue batteryValue = null;
    BatteryReceiver batteryReceiver = null;
    LinearLayout batteryTextLayout;
    TextView batteryTextView;
    BatteryValue reference = null;

    public BatteryIconView(Context context, AttributeSet attrs) {
        super(context, attrs);

        settings = new Settings(context);
        init(context, attrs);

        BatteryReferenceViewModel.observe(context, reference -> {
            this.reference = reference;
        });
    }

    private void init(Context context, AttributeSet attrs) {
        this.context = context;

        //init text view layout
        batteryTextLayout = new LinearLayout(context); //delegates attributes to text subview
        batteryTextView = new TextView(context, attrs);
        batteryTextView.setPadding(0, 0, 0, 0); // no padding for sub view
        batteryTextView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );
        batteryTextLayout.addView(batteryTextView);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setupBatteryReceiver();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (batteryReceiver != null) {
            try {
                context.unregisterReceiver(batteryReceiver);
            } catch (IllegalArgumentException e) {
                // never mind battery receiver was not registered
            } finally {
                batteryReceiver = null;
            }
        }
    }

    void setupBatteryReceiver() {
        batteryReceiver = new BatteryReceiver();
        context.registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public void setColor(int color) {
        customColor = color;
        colorFilter = new LightingColorFilter(customColor, 1);
        batteryTextView.setTextColor(customColor);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final float aspectRatio = 0.35f;
        final Paint.FontMetrics fm = batteryTextView.getPaint().getFontMetrics();

        batteryIconHeight = (fm.bottom - fm.ascent); // (fm.top decreases upwards and is negative)
        batteryIconWidth = Utility.getNearestEvenIntValue((aspectRatio * batteryIconHeight));

        batteryTextOffsetX = batteryIconWidth + getPaddingLeft() + getPaddingRight();
        batteryTextLayout.measure(widthMeasureSpec - (int) batteryIconWidth, heightMeasureSpec);
        final int measuredWidth = (int) (batteryTextLayout.getMeasuredWidth() + batteryTextOffsetX);
        setMeasuredDimension(measuredWidth, batteryTextLayout.getMeasuredHeight());
    }

    @Override
    public void onDraw(Canvas canvas) {

        if (batteryValue == null || getVisibility() != View.VISIBLE) {
            return;
        }

        drawBatteryIcon(canvas);
        drawBatteryTextView(canvas);

    }

    public boolean shallBeVisible() {

        if (batteryValue == null) {
            return false;
        }

        if (!batteryValue.isCharging) return false;
        return batteryValue.getPercentage() < VALUE_FULLY_CHARGED;

    }

    private void drawBatteryIcon(Canvas canvas) {
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setColorFilter(colorFilter);

        paint.setAlpha(255);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.f);

        final Paint.FontMetrics fm = batteryTextView.getPaint().getFontMetrics();
        final float iconBottom = canvas.getHeight() - fm.bottom; // baseline
        final float iconTop = canvas.getHeight() - (-fm.ascent); // upper y (fm.top decreases upwards)
        final float iconHeight = iconBottom - iconTop;

        // fill part of the rect to visualize battery level
        paint.setStyle(Paint.Style.FILL);
        paint.setColor((batteryValue.isCharging) ? Color.GRAY : Color.WHITE);
        final int filled = (int) (batteryValue.levelNormalized * iconHeight);
        canvas.drawRect(getPaddingLeft(), iconBottom - filled, getPaddingLeft() + batteryIconWidth, iconBottom, paint);

        // draw battery border
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        canvas.drawRect(getPaddingLeft(), iconTop, getPaddingLeft() + batteryIconWidth, iconBottom, paint);

        // draw positive pole
        final int poleDistance = (int) batteryIconWidth / 3;
        final float poleLeft = getPaddingLeft() + poleDistance;
        final float poleRight = getPaddingLeft() + (int) batteryIconWidth - poleDistance;
        final float poleTop = iconTop - (0.1f * batteryIconHeight);

        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawRect(poleLeft, poleTop, poleRight, iconTop, paint);

        // if charging, draw lightning symbol using paths
        if (batteryValue.isCharging) {
            // draw stroke using a path
            final float iconLeft = getPaddingLeft();
            paint.setStyle(Paint.Style.STROKE);
            // upper part of stroke (nearly a parallelogram)
            Path p = new Path();
            p.moveTo(iconLeft + 0.4f * batteryIconWidth, iconTop + 0.1f * batteryIconHeight);
            p.lineTo(iconLeft + 0.6f * batteryIconWidth, iconTop + 0.1f * batteryIconHeight);
            p.lineTo(iconLeft + 0.45f * batteryIconWidth, iconTop + 0.25f * batteryIconHeight);
            p.lineTo(iconLeft + 0.3f * batteryIconWidth, iconTop + 0.25f * batteryIconHeight);
            p.lineTo(iconLeft + 0.4f * batteryIconWidth, iconTop + 0.1f * batteryIconHeight);
            canvas.drawPath(p, paint);

            // lower part (a triangle)
            p = new Path();
            float lowerOffsetTop = 0.15f;
            float lowerOffsetLeft = 0.05f;
            p.moveTo(iconLeft + (0.4f + lowerOffsetLeft) * batteryIconWidth, iconTop + (0.1f + lowerOffsetTop) * batteryIconHeight);
            p.lineTo(iconLeft + (0.6f + lowerOffsetLeft) * batteryIconWidth, iconTop + (0.1f + lowerOffsetTop) * batteryIconHeight);
            p.lineTo(iconLeft + (0.3f + lowerOffsetLeft) * batteryIconWidth, iconTop + (0.3f + lowerOffsetTop) * batteryIconHeight);
            p.lineTo(iconLeft + (0.4f + lowerOffsetLeft) * batteryIconWidth, iconTop + (0.1f + lowerOffsetTop) * batteryIconHeight);
            canvas.drawPath(p, paint);
        }
    }

    private void drawBatteryTextView(Canvas canvas) {
        batteryTextLayout.layout(0, 0, canvas.getWidth(), canvas.getHeight());

        // move text view behind batterie icon
        canvas.translate(batteryTextOffsetX, 0);
        batteryTextLayout.draw(canvas);
    }

    private void updateBatteryViewText() {
        float percentage = batteryValue.getPercentage();

        String percentage_string = "";
        if (batteryValue != null) {
            percentage_string = String.format("%d%%", (int) batteryValue.getPercentage());
        }
        String estimate_string = "";

        if (reference != null && batteryValue != null) {
            if (batteryValue.isCharging) {
                if (percentage < VALUE_FULLY_CHARGED) {
                    long est = batteryValue.getEstimateMillis(reference) / 1000; // estimated seconds
                    estimate_string = formatEstimate(est);
                }
            } else { // not charging
                long est = batteryValue.getDischargingEstimateMillis(reference) / 1000; // estimated seconds
                estimate_string = formatEstimate(est);
            }
        }

        batteryTextView.setText(String.format("%s%s", percentage_string, estimate_string));
        batteryTextView.setEllipsize(TextUtils.TruncateAt.END);
        batteryTextView.invalidate();
        requestLayout();
        invalidate();
    }

    private String formatEstimate(long est) {
        if (est > 0) {
            long h = est / 3600;
            long m = (est % 3600) / 60;
            String hour = "";
            String min = "";
            if (h > 0L) {
                hour = String.format("%d%s ", h, context.getString(R.string.hour));
            }
            if (m > 0L) {
                min = String.format("%d%s ", m, context.getString(R.string.minute));
            }

            return String.format(" (%s%s%s)", hour, min, context.getString(R.string.remaining));
        }
        return "";
    }

    class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            batteryValue = new BatteryStats(context, intent).reference;
            BatteryReferenceViewModel.updateIfNecessary(context, batteryValue);
            updateBatteryViewText();
        }
    }
}
