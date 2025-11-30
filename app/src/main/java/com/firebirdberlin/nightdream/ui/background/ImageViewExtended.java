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

package com.firebirdberlin.nightdream.ui.background;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;
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
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
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
        Log.d(TAG, "setImage(uri)");
        String mimeType = context.getContentResolver().getType(uri);
        Log.d(TAG, "mimeType: " + ((mimeType != null) ? mimeType : "null"));
        if ("image/gif".equals(mimeType)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                try {
                    Drawable image = ImageDecoder.decodeDrawable(
                            ImageDecoder.createSource(context.getContentResolver(), uri)
                    );
                    setImageDrawable(image);
                    if (image instanceof AnimatedImageDrawable) {
                        ((AnimatedImageDrawable) image).start();
                    }
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
            } else {
                try {
                    gif = new GifMovie(context.getContentResolver().openInputStream(uri));
                    setBitmap(uri);
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
                if (gif != null) {
                    gif.setOneShot(false);
                    setImageDrawable(gif);
                    gif.setVisible(true, true);
                    gif.start();
                } else {
                    setImageDrawable(new ColorDrawable(Color.BLACK));
                }
            }
        } else {
            setImageDrawable(loadBackgroundImage(uri));
        }
    }

    private Drawable loadBackgroundImage(Uri uri) {
        Log.d(TAG, "loadBackgroundImage()");
        try {
            Bitmap bgimage;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                bgimage = ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(context.getContentResolver(), uri),
                        (decoder, info, source1) -> decoder.setMutableRequired(true)
                );
            } else {
                bgimage = loadBackgroundBitmap(uri);
            }

            if (bgimage != null) {
                bgimage = rescaleBackgroundImage(bgimage);
                setBitmap(bgimage);
                return new BitmapDrawable(context.getResources(), bgimage);
            }
        } catch (OutOfMemoryError e) {
            Toast.makeText(
                    context, "Out of memory. Please, try to scale down your image.",
                    Toast.LENGTH_LONG
            ).show();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return new ColorDrawable(Color.BLACK);
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
}
