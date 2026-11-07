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

package com.firebirdberlin.nightdream.ui;

import static androidx.appcompat.app.AppCompatActivity.RESULT_OK;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.FileUri;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ManageFontsDialogFragment extends AppCompatDialogFragment {
    private static final String TAG = ManageFontsDialogFragment.class.getSimpleName();
    protected File DIRECTORY = null;

    String[] defaultFonts;
    ManageFontsDialogListener mListener;
    ListView listView;
    Button btnAddCustomFont;
    FontAdapter arrayAdapter;
    Uri selectedUri;
    boolean isPurchased = false;

    private final ActivityResultLauncher<Intent> selectFontLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (DIRECTORY == null) return;
                    Uri uri = data.getData();
                    if (uri == null) return;

                    Context context = getContext();
                    if (context == null) return;
                    ContentResolver contentResolver = context.getContentResolver();
                    try (Cursor returnCursor = contentResolver.query(uri, null, null, null, null)) {
                        if (returnCursor == null) return;

                        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (!returnCursor.moveToFirst()) return;

                        String fileName = returnCursor.getString(nameIndex);

                        if (extensionIs(fileName, ".zip")) {
                            extractZipFile(uri);
                        } else if (extensionIs(fileName, ".ttf", ".otf")) {
                            copyFileToDirectory(uri, fileName);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing selected font file", e);
                    }
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    selectCustomFont();
                }
            });

    private static void copyFile(FileInputStream src, FileOutputStream dst) throws IOException {

        try (FileChannel inChannel = src.getChannel(); FileChannel outChannel = dst.getChannel()) {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
    }

    public static String getUserFriendlyFileName(String name) {
        // make an eye-friendly name
        name = name.replace("_", " ");
        name = name.replaceAll(".(?i)ttf", "");
        name = name.replaceAll(".(?i)otf", "");

        return toTitleCase(name);
    }

    private static String toTitleCase(String input) {
        String[] words = input.toLowerCase().split(" ");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];

            if (i > 0 && !word.isEmpty()) {
                builder.append(" ");
            }

            String cap = word.substring(0, 1).toUpperCase() + word.substring(1);
            builder.append(cap);
        }
        return builder.toString();
    }

    public void setIsPurchased(boolean isPurchased) {
        this.isPurchased = isPurchased;
    }

    public File[] listFiles() {
        if (DIRECTORY == null) return null;
        return DIRECTORY.listFiles();
    }

    public void setSelectedUri(String uriString) {
        selectedUri = Uri.parse(uriString);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getContext();
        if (context == null) {
            return super.onCreateDialog(savedInstanceState);
        }
        DIRECTORY = context.getDir("Fonts", Context.MODE_PRIVATE);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DialogTheme);
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.manage_alarm_sounds_dialog, null);
        listView = view.findViewById(R.id.listView);
        btnAddCustomFont = view.findViewById(R.id.addCustomAlarmTone);

        String btnTxt = context.getString(R.string.add_custom_font);
        if (!isPurchased) {
            String productName = context.getString(R.string.product_name_pro);
            btnTxt += "\n (" + productName + ")";
            int color = Utility.getRandomMaterialColor(context);
            int textColor = Utility.getContrastColor(color);
            btnAddCustomFont.setBackgroundColor(color);
            btnAddCustomFont.setTextColor(textColor);
        }
        btnAddCustomFont.setText(btnTxt);

        btnAddCustomFont.setOnClickListener(view1 -> {

            if (!isPurchased) {
                if (mListener != null) {
                    mListener.onPurchaseRequested();
                    dismiss();
                    return;
                }
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                    && !hasPermission()) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                return;
            }
            selectCustomFont();
        });

        initListView();
        builder.setTitle(R.string.typeface)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    if (arrayAdapter != null) {
                        if (mListener != null) {
                            FileUri fileUri = arrayAdapter.getSelectedUri();
                            if (fileUri != null) {
                                mListener.onFontSelected(fileUri.uri, fileUri.name);
                            }
                        }
                        arrayAdapter.release();
                    }
                })
                .setNeutralButton(android.R.string.cancel, (dialog, id) -> {
                    if (arrayAdapter != null) {
                        arrayAdapter.release();
                    }
                });

        return Utility.createDialogTheme(builder.create());
    }

    private boolean hasPermission() {
        Context context = getContext();
        if (context == null) return false;
        return context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void selectCustomFont() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/*");
        selectFontLauncher.launch(intent);
    }

    private void initListView() {
        ArrayList<FileUri> staticFiles = getCustomFiles();

        Log.i(TAG, DIRECTORY.getAbsolutePath());
        final File[] file_list = listFiles();
        if (file_list != null) {
            for (File file : file_list) {
                FileUri fileUri = new FileUri(file);
                fileUri.name = getUserFriendlyFileName(fileUri.name);
                staticFiles.add(fileUri);
            }

        }
        arrayAdapter =
                new FontAdapter(getActivity(), R.layout.list_item_alarm_tone, staticFiles);
        arrayAdapter.setSelectedUri(selectedUri);
        arrayAdapter.setOnDeleteRequestListener(file -> {
            if (file == null || file.uri == null) {
                return;
            }
            String path = file.uri.getPath();
            if (path == null) {
                return;
            }
            File f = new File(path);
            if (f.exists()) {
                boolean deleted = f.delete();
                if (!deleted) {
                    Log.w(TAG, "Failed to delete file: " + path);
                }
            }
        });
        listView.setAdapter(arrayAdapter);
    }

    public void setDefaultFonts(String... fileNames) {
        this.defaultFonts = fileNames;
    }

    public ArrayList<FileUri> getCustomFiles() {

        ArrayList<FileUri> list = new ArrayList<>();
        if (defaultFonts != null) {
            for (String name : defaultFonts) {
                list.add(getFileUri(name));
            }
        }
        return list;
    }

    private FileUri getFileUri(String file) {
        Uri uri = Uri.parse("file:///android_asset/fonts/" + file);
        return new FileUri(uri, getUserFriendlyFileName(file));
    }

    private void copyFileToDirectory(Uri uri, String fileName) {
        Context context = getContext();
        if (context == null) return;
        ContentResolver contentResolver = context.getContentResolver();
        File dstFile = new File(DIRECTORY, fileName);
        FileUri uriDst = new FileUri(dstFile);

        boolean isCreated;
        try {
            isCreated = dstFile.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, "Error creating destination file", e);
            return;
        }
        if (!isCreated) {
            return;
        }

        try (FileInputStream src = (FileInputStream) contentResolver.openInputStream(uri);
             FileOutputStream dst = (FileOutputStream) contentResolver.openOutputStream(uriDst.uri)) {
            if (src != null && dst != null) {
                copyFile(src, dst);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error copying file", e);
            return;
        }

        addFileToList(uriDst);
    }

    private void extractZipFile(Uri uri) {
        Context context = getContext();
        if (context == null) return;
        ContentResolver contentResolver = context.getContentResolver();

        InputStream is;
        ZipInputStream zis;
        try {
            String filename;
            is = contentResolver.openInputStream(uri);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null) {
                filename = ze.getName();
                Log.i(TAG, filename);
                if (!extensionIs(filename, ".ttf", ".otf")) continue;
                String[] parts = filename.split("/");
                if (parts.length > 0) {
                    filename = parts[parts.length - 1];
                }
                filename = filename.replace(" ", "_");

                final File file = new File(DIRECTORY, filename);
                if (!file.exists()) {
                    FileOutputStream fout = new FileOutputStream(DIRECTORY + "/" + filename);

                    while ((count = zis.read(buffer)) != -1) {
                        fout.write(buffer, 0, count);
                    }

                    FileUri uriDst = new FileUri(Uri.fromFile(file), filename);
                    addFileToList(uriDst);
                    fout.close();
                }

                zis.closeEntry();
            }

            zis.close();
        } catch (IOException e) {
            Log.e(TAG, "Error extracting zip file", e);
        }

    }

    private boolean extensionIs(String filename, String... extensions) {
        if (filename != null) {
            for (String ext : extensions) {
                if (ext == null) continue;
                if (filename.toLowerCase().endsWith(ext)) return true;
            }
        }
        return false;
    }

    private void addFileToList(FileUri uri) {
        if (arrayAdapter != null) {
            // make an eye-friendly name
            uri.name = getUserFriendlyFileName(uri.name);

            arrayAdapter.add(uri);
            arrayAdapter.setSelectedUri(uri.uri);
            arrayAdapter.notifyDataSetChanged();
        }
    }

    public void setOnFontSelectedListener(ManageFontsDialogListener listener) {
        this.mListener = listener;
    }

    public interface ManageFontsDialogListener {
        void onFontSelected(Uri uri, String name);

        void onPurchaseRequested();
    }
}
