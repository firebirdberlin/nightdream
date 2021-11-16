package com.firebirdberlin.nightdream.ui;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;

import com.firebirdberlin.nightdream.Graphics;
import com.firebirdberlin.nightdream.R;

import java.lang.ref.WeakReference;

public class BlurringView extends LinearLayout {
    private static final String TAG = "BlurringView";
    private Context context;

    // Factor to scale the view bitmap with before blurring.
    private float downscaleFactor = 0.12f;

    // Number of blur invalidations to do per second.
    private int mFPS = 60;

    // Corner radius for the blured background
    private int cornerRadius = 0;

    //Set round corners
    private boolean roundTL = false;
    private boolean roundTR = false;
    private boolean roundBL = false;
    private boolean roundBR = false;

    /**
     * Is blur running?
     */
    private boolean running = false;

    /**
     * Is window attached?
     */
    private boolean mAttachedToWindow;

    // Reference to View
    private WeakReference<View> activityView;


    public BlurringView(Context context) {
        super(context, null);
    }

    public BlurringView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BlurringView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    // Choreographer callback that re-draws the blur and schedules another callback.
    private final Choreographer.FrameCallback invalidationLoop = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            invalidate();
            Choreographer.getInstance().postFrameCallbackDelayed(this, 1000 / mFPS);
        }
    };

    //Start BlurLayout
    public void startBlur() {
        Log.d(TAG, "startBlur()");
        if (running) {
            return;
        }

        if (mFPS > 0) {
            running = true;
            Choreographer.getInstance().postFrameCallback(invalidationLoop);
        }
    }

    //Pause Blur
    public void pauseBlur() {
        if (!running) {
            return;
        }

        running = false;
        Choreographer.getInstance().removeFrameCallback(invalidationLoop);
    }

    @Override
    protected void onAttachedToWindow() {
        Log.d(TAG, "onAttachedToWindow()");
        super.onAttachedToWindow();
        mAttachedToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttachedToWindow = false;
        pauseBlur();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (running && checkBlur()) {
            Bitmap bitmap = blur();
            if (bitmap != null) {
                setBackground(new BitmapDrawable(getResources(), getRoundedCornerBitmap(bitmap)));
            }
        }
    }

    private boolean checkBlur() {
        if (getContext() == null || isInEditMode()) {
            return false;
        }

        //Check reference to the parent view
        if (activityView == null || activityView.get() == null) {
            activityView = new WeakReference<>(getActivityView());
            if (activityView.get() == null) {
                return false;
            }
        }

        // return if view size <= 0
        if (activityView.get().getWidth() <= 0 || activityView.get().getHeight() <= 0) {
            return false;
        }

        return true;
    }

    //blur the background of the view
    private Bitmap blur() {
        Point pointRelativeToView = getPositionInScreen();

        // Set View.INVISIBLE before creating the bitmap to blur
        // The view shouldn't be in the bitmap.
        this.setVisibility(View.INVISIBLE);

        //get downscaled bitmap
        Bitmap bitmap = getDownscaledBitmapForView(
                activityView.get(),
                new Rect(
                        pointRelativeToView.x,
                        pointRelativeToView.y,
                        pointRelativeToView.x + getWidth(),
                        pointRelativeToView.y + getHeight()
                ),
                downscaleFactor
        );

        // Blur the bitmap.
        bitmap = Graphics.blur(bitmap);

        // Make self visible again.
        this.setVisibility(View.VISIBLE);

        // return blurred bitmap.
        return bitmap;
    }

    private Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final float densityMultiplier = context.getResources().getDisplayMetrics().density;

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        final float roundPx = cornerRadius * densityMultiplier;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        if (!roundTL) {
            canvas.drawRect(0, 0, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2, paint);
        }
        if (!roundTR) {
            canvas.drawRect((float) bitmap.getWidth() / 2, 0, bitmap.getWidth(), (float) bitmap.getHeight() / 2, paint);
        }
        if (!roundBL) {
            canvas.drawRect(0, (float) bitmap.getHeight() / 2, (float) bitmap.getWidth() / 2, bitmap.getHeight(), paint);
        }
        if (!roundBR) {
            canvas.drawRect((float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2, bitmap.getWidth(), bitmap.getHeight(), paint);
        }

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return output;
    }

    // Returns the position in screen
    private Point getPositionInScreen() {
        PointF pointF = getPositionInScreen(this);
        return new Point((int) pointF.x, (int) pointF.y);
    }

    //Finds Point of the parent view
    private PointF getPositionInScreen(View view) {
        if (getParent() == null) {
            return new PointF();
        }

        ViewGroup parent;
        try {
            parent = (ViewGroup) view.getParent();
        } catch (Exception e) {
            return new PointF();
        }

        if (parent == null) {
            return new PointF();
        }

        PointF point = getPositionInScreen(parent);
        point.offset(view.getX(), view.getY());
        return point;
    }

    // create a downscaled bitmap
    private Bitmap getDownscaledBitmapForView(View view, Rect crop, float downscaleFactor) throws NullPointerException {
        View screenView = view.getRootView();

        int width = (int) (crop.width() * downscaleFactor);
        int height = (int) (crop.height() * downscaleFactor);

        if (width > 0 && height > 0) {
            float dx = -crop.left * downscaleFactor;
            float dy = -crop.top * downscaleFactor;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Matrix matrix = new Matrix();
            matrix.preScale(downscaleFactor, downscaleFactor);
            matrix.postTranslate(dx, dy);
            canvas.setMatrix(matrix);
            screenView.draw(canvas);

            return bitmap;
        }
        return null;
    }

    //Sets downscale factor
    public void setDownscaleFactor(float downscaleFactor) {
        this.downscaleFactor = downscaleFactor;
        invalidate();
    }

    //Get downscale factor.
    public float getDownscaleFactor() {
        return this.downscaleFactor;
    }

    //Sets FPS
    public void setFPS(int fps) {
        if (running) {
            pauseBlur();
        }

        this.mFPS = fps;

        if (mAttachedToWindow) {
            startBlur();
        }
    }

    //Get FPS
    public int getFPS() {
        return this.mFPS;
    }

    // Sets corner radius value.
    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        invalidate();
    }

    // Get corner radius value.
    public int getCornerRadius() {
        return cornerRadius;
    }

    public void setRoundedCorner(boolean roundTL, boolean roundTR, boolean roundBL, boolean roundBR) {
        this.roundTL = roundTL;
        this.roundTR = roundTR;
        this.roundBL = roundBL;
        this.roundBR = roundBR;
    }

    // Casts context to Activity if context is a View or not and return View reference
    private View getActivityView() {
        Context context = this.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content);
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}