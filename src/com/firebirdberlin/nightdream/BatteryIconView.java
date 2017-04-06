package com.firebirdberlin.nightdream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.models.BatteryValue;
import com.firebirdberlin.nightdream.repositories.BatteryStats;


public class BatteryIconView extends View {
    private static final String TAG = "NightDream.BatteryIconView";
    private static float VALUE_FULLY_CHARGED = 95.f;

    Context context;
    Settings settings;
    Paint paint = new Paint();
    int customcolor = Color.GREEN;
    int customSecondaryColor = Color.parseColor("#C2C2C2");
    float batteryTextOffsetX;
    float batteryIconHeight;
    float batteryIconWidth;
    ColorFilter colorFilter;
    BatteryValue batteryValue = null;
    BatteryReceiver batteryReceiver = null;
    LinearLayout batteryTextLayout;
    TextView batteryTextView;

    public BatteryIconView(Context context, AttributeSet attrs) {
        super(context, attrs);

        settings = new Settings(context);

        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        this.context = context;
        colorFilter = new LightingColorFilter(customcolor, 1);

        //init text view layout
        batteryTextLayout = new LinearLayout(context); //delegates attributes to text subview
        batteryTextView = new TextView(context, attrs);
        batteryTextView.setVisibility(View.VISIBLE);
        batteryTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        batteryTextLayout.addView(batteryTextView);

    }

    @Override
    public void onAttachedToWindow(){
        super.onAttachedToWindow();
        setupBatteryReceiver();
    }

    @Override
    public void onDetachedFromWindow(){
        super.onDetachedFromWindow();
        if (batteryReceiver != null) {
            context.unregisterReceiver(batteryReceiver);
            batteryReceiver = null;
        }
    }

    void setupBatteryReceiver() {
        batteryReceiver = new BatteryReceiver();
        context.registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            BatteryStats batteryStats = new BatteryStats(context, intent);
            batteryValue = batteryStats.getBatteryValue();
            updateBatteryViewText();

            Log.i(TAG, String.format("battery level: %d, scale: %d, percent: %f", batteryValue.level, batteryValue.scale, batteryValue.getPercentage()));
        }
    }

    public void setColor(int color) {
        customcolor = color;
        colorFilter = new LightingColorFilter(customcolor, 1);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        batteryTextLayout.measure(widthMeasureSpec, heightMeasureSpec);

        final float aspectRatio = 0.35f;
        //final float aspectRatio = 0.5f;
        //batteryIconHeight = batteryTextLayout.getMeasuredHeight();

        Paint.FontMetrics fm = batteryTextView.getPaint().getFontMetrics();

        //batteryIconHeight = (int) (fm.descent - fm.ascent);
        //batteryIconHeight = (int) -fm.ascent;
        batteryIconHeight = (fm.bottom - fm.ascent); // (fm.top decreases upwards and is negative)
        //batteryIconHeight = (int) batteryTextView.getPaint().getTextSize();
        batteryIconWidth =  (aspectRatio * batteryIconHeight);

        // round to nearest even number
        batteryIconWidth = Utility.getNearestEvenIntValue(batteryIconWidth);

        final int marginRight = 3;
        batteryTextOffsetX = batteryIconWidth + marginRight;
        final int measuredWidth = (int) (batteryTextLayout.getMeasuredWidth() + batteryTextOffsetX);

        setMeasuredDimension(measuredWidth, batteryTextLayout.getMeasuredHeight());
    }

    @Override
    public void onDraw(Canvas canvas) {

        if (batteryValue == null) {
                return;
        }

        drawBatteryIcon(canvas);
        drawBatteryTextView(canvas);

    }

    public boolean shallBeVisible() {

        if (batteryValue == null) {
            return false;
        }

        if (! batteryValue.isCharging ) return false;
        if ( batteryValue.getPercentage() >= VALUE_FULLY_CHARGED ) return false;

        return true;
    }

    private void drawBatteryIcon(Canvas canvas) {
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setColorFilter(colorFilter);
        paint.setColor(Color.WHITE);
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.f);

        //int top = (int) (0.15 * batteryIconHeight);
        Paint.FontMetrics fm = batteryTextView.getPaint().getFontMetrics();
        //int top = (int) (batteryIconHeight - fm.bottom);
        //int main_height = batteryIconHeight - top;
        //int main_height = batteryIconHeight;
        //canvas.drawRect((int) (0.5 * batteryIconWidth - 0.1 * batteryIconWidth), 0, (int) (0.5 * batteryIconWidth + 0.1 * batteryIconWidth), top, paint);
        //canvas.drawRect(0, top, batteryIconWidth, main_height, paint);

        //metrics
        float iconBottom = canvas.getHeight() - fm.bottom; // baseline
        //canvas.drawLine(0, yTextBaseline, 10, yTextBaseline, paint);
        float iconTop = canvas.getHeight() - (-fm.ascent); // upper y (fm.top decreases upwards)
        float iconHeight = iconBottom - iconTop;

        //canvas.drawLine(0, yTextTop, 10, yTextTop, paint);

        canvas.drawRect(0, iconTop, batteryIconWidth, iconBottom, paint);
        // draw pluspol
        canvas.drawRect((int) (0.5 * batteryIconWidth - 0.1 * batteryIconWidth), iconTop - (0.1f * batteryIconHeight), (int) (0.5 * batteryIconWidth + 0.1 * batteryIconWidth), iconTop, paint);

        paint.setStyle(Paint.Style.FILL);

        int filled = (int) (batteryValue.levelNormalized * iconHeight);
        canvas.drawRect(0, iconBottom - filled, batteryIconWidth, iconBottom, paint);
    }

    private void drawBatteryTextView(Canvas canvas) {
        batteryTextLayout.layout(0, 0, canvas.getWidth(), canvas.getHeight());
        // move text view behind batterie icon
        canvas.translate(batteryTextOffsetX, 0);
        batteryTextLayout.draw(canvas);
    }

    private void updateBatteryViewText() {
        BatteryValue reference = settings.loadBatteryReference();
        float percentage = batteryValue.getPercentage();

        String percentage_string = "";
        if (batteryValue != null) {
            percentage_string = String.format("%d%%", (int) batteryValue.getPercentage());
        }
        String estimate_string = "";

        if (batteryValue.isCharging) {
            if (percentage < VALUE_FULLY_CHARGED){
                //long est = batteryValue.getEstimateMillis(reference)/1000; // estimated seconds
                long est = 300;
                estimate_string = formatEstimate(est);
            }
        } else { // not charging
            long est = batteryValue.getDischargingEstimateMillis(reference)/1000; // estimated seconds
            estimate_string = formatEstimate(est);
        }

        batteryTextView.setText(percentage_string + estimate_string);
        invalidate();
    }

    private String formatEstimate(long est) {
        Log.i(TAG, String.valueOf(est));
        if (est > 0){
            long h = est / 3600;
            long m  = ( est % 3600 ) / 60;
            String hour = "";
            String min = "";
            if ( h > 0L ) {
                hour = String.format("%d%s ", h, context.getString(R.string.hour));
            }
            if ( m > 0L ) {
                min = String.format("%d%s ", m, context.getString(R.string.minute));
            }

            return String.format(" (%s%s%s)", hour, min, context.getString(R.string.remaining));
        }
        return "";
    }
}
