package com.firebirdberlin.nightdream.services;

import android.app.ForegroundServiceStartNotAllowedException;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.PreferencesActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.CopyImagesDataHolder;

import java.io.File;
import java.util.List;

public class ImageCopyService extends Service {

    private static final String TAG = "ImageCopyService";
    private final IBinder binder = new LocalBinder();
    private int imageProcessed = 0;
    private int urisSize = 0;
    private boolean running = false;

    public class LocalBinder extends Binder {
        public ImageCopyService getService() {
            return ImageCopyService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public boolean getStatus() {
        return running;
    }

    public int getImageProcessed() {
        return imageProcessed;
    }

    public int getUrisSize() {
        return urisSize;
    }

    public void copyImages(PreferencesActivity mContext, List<Uri> uris, File directory) {
        NotificationManager notificationManager;

        try {

            NotificationCompat.Builder notification =
                    new NotificationCompat.Builder(this, Config.NOTIFICATION_CHANNEL_ID_COPYMSG)
                            // Create the notification to display while the service
                            // is running
                            .setOngoing(true)
                            .setAutoCancel(true)
                            .setContentTitle(getString(R.string.app_name))
                            .setContentText(getString(R.string.images_background_copy))
                            .setSmallIcon(R.drawable.ic_clock);

            int type = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
            }

            ServiceCompat.startForeground(
                    /* service = */ this,
                    /* id = */ 100, // Cannot be 0
                    /* notification = */ notification.build(),
                    /* foregroundServiceType = */ type);

            imageProcessed = 0;
            urisSize = uris.size();
            CopyImagesDataHolder.getInstance().updateImageUriSize(urisSize);

            //Copy images in the background
            new Thread(() -> {
                // do background stuff here
                running = true;
                CopyImagesDataHolder.getInstance().updateImageCopyServiceStatus(running);

                for (Uri uri : uris) {
                    imageProcessed += 1;
                    CopyImagesDataHolder.getInstance().updateImageProcessed(imageProcessed);
                    String name = "image_" + imageProcessed + ".jpg";
                    Utility.copyToDirectory(mContext, uri, directory, name);

                    Log.d(TAG, "Copy Image: " + imageProcessed + " / " + urisSize);
                }
                // OnPostExecute stuff here
                Log.d(TAG, "All images processed");
                running = false;
                CopyImagesDataHolder.getInstance().updateImageCopyServiceStatus(running);
            }).start();
        } catch (Exception ex) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ex instanceof ForegroundServiceStartNotAllowedException) {
                Log.e(TAG, ex.toString());

            }
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
