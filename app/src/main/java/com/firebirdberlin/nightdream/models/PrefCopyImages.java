package com.firebirdberlin.nightdream.models;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class PrefCopyImages{

    private static final String TAG = "PrefCopyImages";
    private static PrefCopyImages instance;
    private final MutableLiveData<Integer> imageProcessed = new MutableLiveData<>();
    private final MutableLiveData<Boolean> imageCopyServiceStatus = new MutableLiveData<>();
    private final MutableLiveData<Integer> imageUriSize = new MutableLiveData<>();

    public PrefCopyImages() {
        imageProcessed.postValue(0);
        imageCopyServiceStatus.postValue(false);
        imageUriSize.postValue(0);
    }

    public static synchronized PrefCopyImages getInstance() {
        if (instance == null) instance = new PrefCopyImages();
        return instance;
    }

    public void updateImageProcessed(Integer newData) {
        Log.d(TAG,"imageProcessed changed: "+newData);
        imageProcessed.postValue(newData); // from thread to service
    }

    public void updateImageCopyServiceStatus(Boolean newData) {
        Log.d(TAG,"imageCopyServiceStatus changed: "+newData);
        imageCopyServiceStatus.postValue(newData); // from thread to service
    }

    public void updateImageUriSize(Integer newData) {
        Log.d(TAG,"updateImageUriSize changed: "+newData);
        imageUriSize.postValue(newData); // from thread to service
    }

    public LiveData<Integer> getImageProcessed() {
        return imageProcessed;
    }

    public LiveData<Boolean> getImageCopyServiceStatus() {
        return imageCopyServiceStatus;
    }

    public LiveData<Integer> getImageUriSize() {
        return imageUriSize;
    }

}
