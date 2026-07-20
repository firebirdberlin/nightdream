package com.firebirdberlin.nightdream.models;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class CopyImagesDataHolder {

    private static final String TAG = "CopyImagesDataHolder";
    private static CopyImagesDataHolder instance;
    private final MutableLiveData<Integer> imageProcessed = new MutableLiveData<>();
    private final MutableLiveData<Boolean> imageCopyServiceStatus = new MutableLiveData<>();
    private final MutableLiveData<Integer> imageUriSize = new MutableLiveData<>();

    public CopyImagesDataHolder() {
        imageProcessed.postValue(0);
        imageCopyServiceStatus.postValue(false);
        imageUriSize.postValue(0);
    }

    public static synchronized CopyImagesDataHolder getInstance() {
        if (instance == null) instance = new CopyImagesDataHolder();
        return instance;
    }

    public void updateImageProcessed(Integer intProcessedImages) {
        Log.d(TAG,"imageProcessed changed: "+intProcessedImages);
        imageProcessed.postValue(intProcessedImages); // from thread to service
    }

    public void updateImageCopyServiceStatus(Boolean boolImageCopyServiceStatus) {
        Log.d(TAG,"imageCopyServiceStatus changed: "+boolImageCopyServiceStatus);
        imageCopyServiceStatus.postValue(boolImageCopyServiceStatus); // from thread to service
    }

    public void updateImageUriSize(Integer intImageUriSize) {
        Log.d(TAG,"updateImageUriSize changed: "+intImageUriSize);
        imageUriSize.postValue(intImageUriSize); // from thread to service
    }

    //Returns the number of currently processed images
    public LiveData<Integer> getImageProcessed() {
        return imageProcessed;
    }

    //Returns the status of the Service
    //@return: true = running, false = not running
    public LiveData<Boolean> getImageCopyServiceStatus() {
        return imageCopyServiceStatus;
    }

    //Returns the maximum number of images to be processed.
    public LiveData<Integer> getImageUriSize() {
        return imageUriSize;
    }

}
