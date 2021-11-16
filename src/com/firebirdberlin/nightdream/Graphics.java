package com.firebirdberlin.nightdream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

import com.google.android.renderscript.Toolkit;

public class Graphics {

    public static Bitmap blur(Context context, Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int blurRadius = 15;
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);

        return Toolkit.INSTANCE.blur(bitmap, blurRadius);
    }

    public static Bitmap sketch(final Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        Bitmap output = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int imageHeight = output.getHeight();
        int imageWidth = output.getWidth();
        for (int i = 0; i < imageWidth; i++) {

            for (int j = 0; j < imageHeight; j++) {
                int oldPixel = bitmap.getPixel(i, j);

                int oldRed = Color.red(oldPixel);
                int oldBlue = Color.blue(oldPixel);
                int oldGreen = Color.green(oldPixel);
                int oldAlpha = Color.alpha(oldPixel);

                int newRed = 0;
                int newBlue = 0;
                int newGreen = 0;

                if (((oldRed + oldBlue + oldGreen) / 3) > 150) {
                    newRed = newBlue = newGreen = 255;
                } else if (((oldRed + oldBlue + oldGreen) / 3) > 100) {
                    newRed = newBlue = newGreen = 150;
                }

                int newPixel = Color.argb(oldAlpha, newRed, newGreen, newBlue);
                output.setPixel(i, j, newPixel);
            }
        }
        return output;
    }

    public static Bitmap desaturate(final Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        Bitmap output = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();

        final ColorMatrix matrixGray = new ColorMatrix();
        matrixGray.setSaturation(0);
        paint.setColorFilter(new ColorMatrixColorFilter(matrixGray));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return output;
    }

    public static Bitmap sepia(final Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        Bitmap output = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();

        //filter: sepia
        final ColorMatrix matrixA = new ColorMatrix();
        final ColorMatrix matrixB = new ColorMatrix();
        matrixA.setSaturation(0);
        matrixB.setScale(1f, .80f, .52f, 1.0f);
        matrixA.setConcat(matrixB, matrixA);

        paint.setColorFilter(new ColorMatrixColorFilter(matrixA));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return output;
    }

    public static Bitmap invert(final Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        Bitmap output = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(
                new ColorMatrix(new float[]
                        {
                                -1f, 0f, 0f, 0f, 255f,
                                0f, -1f, 0f, 0f, 255f,
                                0f, 0f, -1f, 0f, 255f,
                                0f, 0f, 0f, 1f, 0f
                        })
        ));

        canvas.drawBitmap(bitmap, 0, 0, paint);
        return output;
    }

    public static Bitmap contrast(final Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        Bitmap output = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(
                new ColorMatrix(new float[]
                        {
                                3f, 0f, 0f, 0f, -255f,
                                0f, 3f, 0f, 0f, -255f,
                                0f, 0f, 3f, 0f, -255f,
                                0f, 0f, 0f, 1f, 0f
                        })
        ));

        canvas.drawBitmap(bitmap, 0, 0, paint);
        return output;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight
    ) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static int setColorWithAlpha(int color, int alpha) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }
}
