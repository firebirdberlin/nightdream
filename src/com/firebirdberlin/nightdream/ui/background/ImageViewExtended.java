package com.firebirdberlin.nightdream.ui.background;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.Graphics;
import com.firebirdberlin.nightdream.Utility;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageViewExtended extends AppCompatImageView {
    private static final String TAG = "ImageViewExtended";
    private final Context context;
    private GifMovie gif = null;
    private PDFMovie pdf = null;
    private Bitmap bitmapImage;


    public ImageViewExtended(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    public ImageViewExtended(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public ImageViewExtended(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    public Bitmap getBitmap() {
        return bitmapImage;
    }

    private void setBitmap(Uri uri) {
        try {
            bitmapImage = loadBackgroundBitmap(uri);
            bitmapImage = rescaleBackgroundImage(bitmapImage);
        } catch (OutOfMemoryError e) {
            Toast.makeText(
                    context, "Out of memory. Please, try to scale down your image.",
                    Toast.LENGTH_LONG
            ).show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setBitmap(Drawable imageDrawable) {
        bitmapImage = ((BitmapDrawable) imageDrawable).getBitmap();
    }

    private void setBitmap(Bitmap bitmapImage) {
        this.bitmapImage = bitmapImage;
    }

    public void startDrawableAnimation() {
        if (this.getDrawable() instanceof Animatable) {
            ((Animatable) this.getDrawable()).start();
        }
    }

    public void setText(String text) {
        setText(text, 16, Color.BLACK, null, 0);
    }

    public void setText(String text, float textSize, int textColor) {
        setText(text, textSize, textColor, null, 0);
    }

    public void setText(String text, float textSize, int textColor, Typeface typeFace, int glowRadius) {
        Log.d(TAG, "setText()");

        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG
                | Paint.LINEAR_TEXT_FLAG);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize * getResources().getDisplayMetrics().density);
        textPaint.setShadowLayer(glowRadius, 0, 0, textColor);
        if (typeFace == null) {
            textPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        } else {
            textPaint.setTypeface(typeFace);
        }

        int width = (int) textPaint.measureText(text);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        if (width > size.x) {
            width = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_PX,
                    size.x,
                    context.getResources().getDisplayMetrics());
        }

        StaticLayout mTextLayout;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, width)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0, 1)
                    .setIncludePad(false)
                    .setMaxLines(5);
            mTextLayout = builder.build();
        } else {
            mTextLayout = new StaticLayout(text, textPaint,
                    width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        }

        Bitmap image = Bitmap.createBitmap(width, mTextLayout.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mTextLayout.draw(canvas);

        setImage(new BitmapDrawable(getResources(), image));
    }

    public void setImage(Drawable image) {
        Log.d(TAG, "setImage(Drawable)");
        setImageDrawable(image);
    }

    public void setImage(Uri uri) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());

        switch (extension){
            case "pdf":
                Log.d(TAG, "pdf");
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());
                executor.execute(() -> { //background thread
                    pdf = new PDFMovie(context,getResources(),uri);
                    handler.post(() -> { //like onPostExecute()
                        setImageDrawable(pdf);
                        this.post(() -> pdf.start());
                    });
                });
            break;

            case "gif":
                Log.d(TAG, "gif");
                try {
                    gif = new GifMovie(context.getContentResolver().openInputStream(uri));
                    setBitmap(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (gif != null) {
                    gif.setOneShot(false);
                    setImageDrawable(gif);
                    gif.setVisible(true, true);
                    gif.start();
                } else {
                    setImageDrawable(new ColorDrawable(Color.BLACK));
                }
                break;

            default:
                Log.d(TAG,"default");
                setImageDrawable(loadBackgroundImage(uri));
        }
    }

    private Drawable loadBackgroundImage(Uri uri) {
        Log.d(TAG, "loadBackgroundImage()");
        try {
            Drawable cached = loadBackgroundImageFromCache();
            if (cached != null) {
                Log.d(TAG, "load cached background");
                setBitmap(cached);
                return cached;
            }

            Bitmap bgimage = loadBackgroundBitmap(uri);
            bgimage = rescaleBackgroundImage(bgimage);
            writeBackgroundImageToCache(bgimage);
            if (bgimage != null) {
                setBitmap(bgimage);
                return new BitmapDrawable(context.getResources(), bgimage);
            }
        } catch (OutOfMemoryError e) {
            Toast.makeText(
                    context, "Out of memory. Please, try to scale down your image.",
                    Toast.LENGTH_LONG
            ).show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ColorDrawable(Color.BLACK);
    }

    public boolean existCacheFile() {
        File cacheFile = new File(context.getCacheDir(), Config.backgroundImageCacheFilename);
        return cacheFile.exists();
    }

    public void bitmapUriToCache(Uri uri) {
        try {
            Bitmap bgimage = loadBackgroundBitmap(uri);
            bgimage = rescaleBackgroundImage(bgimage);
            writeImageToCache(bgimage);
        } catch (OutOfMemoryError e) {
            Toast.makeText(
                    context, "Out of memory. Please, try to scale down your image.",
                    Toast.LENGTH_LONG
            ).show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private BitmapDrawable loadBackgroundImageFromCache() {
        Log.d(TAG, "loadBackgroundImageFromCache");
        File cacheFile = new File(context.getCacheDir(), Config.backgroundImageCacheFilename);
        if (cacheFile.exists()) {
            Bitmap bgimage = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
            Log.d(TAG, "loading image from cache");
            return new BitmapDrawable(context.getResources(), bgimage);
        }
        return null;
    }

    private Bitmap rescaleBackgroundImage(Bitmap bgimage) {
        Log.d(TAG, "rescaleBackgroundImage");
        if (bgimage == null) return null;

        Point display = Utility.getDisplaySize(context);
        int nw = bgimage.getWidth();
        int nh = bgimage.getHeight();
        boolean scaling_needed = false;
        if (bgimage.getHeight() > display.y) {
            nw = (int) ((display.y / (float) bgimage.getHeight()) * bgimage.getWidth());
            nh = display.y;
            scaling_needed = true;
        }

        if (nw > display.x) {
            nh = (int) ((display.x / (float) bgimage.getWidth()) * bgimage.getHeight());
            nw = display.x;
            scaling_needed = true;
        }

        if (scaling_needed) {
            bgimage = Bitmap.createScaledBitmap(bgimage, nw, nh, false);
        }
        return bgimage;
    }

    private Bitmap loadBackgroundBitmap(Uri uri) throws Exception {
        Log.d(TAG, "loadBackgroundBitmap");
        ParcelFileDescriptor parcelFileDescriptor =
                context.getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

        // Calculate inSampleSize
        Point display = Utility.getDisplaySize(context);
        options.inSampleSize = Graphics.calculateInSampleSize(options, display.x, display.y);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        parcelFileDescriptor.close();

        int rotation = Utility.getCameraPhotoOrientation(fileDescriptor);
        if (rotation != 0) {
            bitmap = Utility.rotateBitmap(bitmap, rotation);
        }
        return bitmap;
    }

    private void writeBackgroundImageToCache(Bitmap bitmapToCache) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> writeImageToCache(bitmapToCache));
    }

    private void writeImageToCache(Bitmap bitmapToCache) {
        if (bitmapToCache != null) {
            Log.d(TAG, "writing image to cache");
            File cacheFile = new File(context.getCacheDir(), Config.backgroundImageCacheFilename);
            try {
                FileOutputStream out = new FileOutputStream(cacheFile);
                bitmapToCache.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
