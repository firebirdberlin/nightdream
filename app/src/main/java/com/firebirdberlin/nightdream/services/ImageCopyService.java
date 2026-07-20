package com.firebirdberlin.nightdream.services;

import android.app.ForegroundServiceStartNotAllowedException;

import android.app.Notification;
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
    private Notification notification;
    private int type = 0;

    public class LocalBinder extends Binder {
        public ImageCopyService getService() {
            return ImageCopyService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"A client is binding to the service with bindService()");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG,"All clients have unbound with unbindService()");
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Create the notification to display while the service is running
        notification =
                new NotificationCompat.Builder(this, Config.NOTIFICATION_CHANNEL_ID_SERVICES)
                        .setOngoing(true)
                        .setAutoCancel(true)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.images_background_copy))
                        .setSmallIcon(R.drawable.ic_clock)
                        .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
        }
    }

    //Returns the status of the Service
    //@return: true = running, false = not running
    public boolean getStatus() {
        return running;
    }

    //Returns the number of currently processed images
    public int getImageProcessed() {
        return imageProcessed;
    }

    //Returns the maximum number of images to be processed.
    public int getUrisSize() {
        return urisSize;
    }

    public void copyImages(List<Uri> uris, File directory) {
        if (running) {
            Log.d(TAG, "copyImages: already running, ignoring request");
            return;
        }

        try {

            //Checking if the channel is created.
            boolean checkNotificationChannel = Utility.checkNotificationChannel(this, Config.NOTIFICATION_CHANNEL_ID_SERVICES);
            if (!checkNotificationChannel){
                Log.e(TAG, "NotificationChannel not found");
            }

            ServiceCompat.startForeground(
                    /* service = */ this,
                    /* id = */ 100, // Cannot be 0
                    /* notification = */ notification,
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
                    Utility.copyToDirectory(getApplicationContext(), uri, directory, name);

                    Log.d(TAG, "Copy Image: " + imageProcessed + " / " + urisSize);
                }
                // OnPostExecute stuff here
                Log.d(TAG, "All images processed");
                running = false;
                CopyImagesDataHolder.getInstance().updateImageCopyServiceStatus(running);

                Log.d(TAG, "Work done. stopForeground + stopSelf");
                stopForeground(true);
                stopSelf();
            }).start();
        } catch (Exception ex) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ex instanceof ForegroundServiceStartNotAllowedException) {
                Log.e(TAG, ex.toString());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "The service is no longer used and is being destroyed");
    }
}
