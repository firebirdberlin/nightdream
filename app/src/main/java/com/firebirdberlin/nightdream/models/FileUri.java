package com.firebirdberlin.nightdream.models;

import android.net.Uri;

import java.io.File;

public class FileUri {
    public Uri uri;
    public String name;


    public FileUri(File file) {
        this.uri = Uri.fromFile(file);
        this.name = file.getName();
    }

    public FileUri(Uri uri, String name) {
        this.uri = uri;
        this.name = name;
    }
}
