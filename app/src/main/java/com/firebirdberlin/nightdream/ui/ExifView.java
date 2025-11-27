package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.exifinterface.media.ExifInterface;

import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.databinding.ExifViewBinding;
import com.firebirdberlin.openweathermapapi.GeocoderApi;
import com.firebirdberlin.openweathermapapi.models.City;

import java.io.File;
import java.io.IOException;

public class ExifView {
    private final static String TAG = "ExifView";
    private final ExifViewBinding exifBinding;

    public ExifView(Context mContext) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        exifBinding = ExifViewBinding.inflate(inflater);
    }

    public View getView() {
        return exifBinding.getRoot();
    }

    public Boolean getExifView(Context mContext, File file, int secondaryColor) {
        Log.d(TAG, "getExifInformation");

        if (file == null) {
            return false;
        }

        try {
            String exifDate = "";
            String exifTime = "";
            String exifCity = "";
            String exifCountry = "";

            exifBinding.setExifTextColor(secondaryColor);

            ExifInterface exif = new ExifInterface(file);

            String tagDateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
            if (tagDateTime != null) {
                String[] exifDateTime = tagDateTime.split(" ");
                String[] exifDateSplit = exifDateTime[0].split(":");
                exifDate = exifDateSplit[2] + "." + exifDateSplit[1] + "." + exifDateSplit[0];
                exifTime = exifDateTime[1];
            }

            String tagGpsLatitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String tagGpsLongitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            if (tagGpsLatitude != null && tagGpsLongitude != null) {
                String[] separatedLat = tagGpsLatitude.split(",");
                String[] separatedLong = tagGpsLongitude.split(",");
                double lat = convertArcMinToDegrees(separatedLat);
                double lon = convertArcMinToDegrees(separatedLong);
                City city = GeocoderApi.findCityByCoordinates(mContext, lat, lon);
                if (city != null) {
                    exifCity = city.name;
                    exifCountry = city.countryName;
                }
            }

            exifBinding.setExifDate(exifDate);
            exifBinding.setExifTime(exifTime);
            exifBinding.setExifCity(exifCity);
            exifBinding.setExifCountry(exifCountry);

            return true;
        } catch (IOException | IndexOutOfBoundsException e) {
            Log.e(TAG, "exception: ", e);
            return false;
        }
    }

    private double convertArcMinToDegrees(String[] separated) {
        double convert;
        String[] separated2 = separated[2].split("/");
        convert = Double.parseDouble(separated2[0]) / Double.parseDouble(separated2[1]) / 60;
        String[] separated1 = separated[1].split("/");
        convert = (Double.parseDouble(separated1[0]) + convert) / Double.parseDouble(separated1[1]) / 60;
        String[] separated0 = separated[0].split("/");
        convert = Double.parseDouble(separated0[0]) + convert;
        return convert;
    }
}