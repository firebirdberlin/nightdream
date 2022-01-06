package com.firebirdberlin.nightdream.ui.background;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.IOException;

public class PDFMovie extends AnimationDrawable {
    private static final String TAG = "PDFMovie";
    private int totalTime;
    private int totalFrames;

    public PDFMovie(Context context, Resources resources, Uri uri) {
        super();
        this.totalTime = 0;
        this.totalFrames = 0;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            try {
                Log.d(TAG, "PDF: " + uri.getPath());

                setVisible(true, true);
                setOneShot(false);
                ParcelFileDescriptor parcelFileDescriptor =
                        context.getContentResolver().openFileDescriptor(uri, "r");

                PdfRenderer renderer = new PdfRenderer(parcelFileDescriptor);

                final int pageCount = renderer.getPageCount();
                for (int i = 0; i < pageCount; i++) {
                    Log.d(TAG, "add page " + i);
                    PdfRenderer.Page page = renderer.openPage(i);

                    //Get Display size
                    WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                    Display display = wm.getDefaultDisplay();
                    Point size = new Point();
                    display.getRealSize(size);
                    int width = size.x;
                    int height = size.y;

                    //Bitmap to save PDF - Document
                    Bitmap pdfDocument = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                    // render for showing on the screen
                    page.render(pdfDocument, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                    //white background for pdfDocument
                    Bitmap pdfImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(pdfImage);
                    canvas.drawColor(Color.WHITE);
                    canvas.drawBitmap(pdfDocument, 0, 0, null);

                    // add bitmap to anmimation
                    addFrame(new BitmapDrawable(resources, pdfImage), 5000);

                    // close the page
                    page.close();
                }
                renderer.close();
            } catch (IOException e) {
                e.printStackTrace();
                setOneShot(true);
                addFrame(new ColorDrawable(Color.BLACK), 0);
            }
        } else {
            setOneShot(true);
            addFrame(new ColorDrawable(Color.BLACK), 0);
        }
    }

    @Override
    public void addFrame(Drawable frame, int duration) {
        super.addFrame(frame, duration);
        totalTime += duration;
        totalFrames++;
    }

    public int getTotalTime() {
        return this.totalTime;
    }

    public int getTotalFrames() {
        return this.totalFrames;
    }

}
