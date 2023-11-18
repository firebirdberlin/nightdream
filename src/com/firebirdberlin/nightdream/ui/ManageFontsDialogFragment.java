package com.firebirdberlin.nightdream.ui;

import static androidx.appcompat.app.AppCompatActivity.RESULT_OK;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.FileUri;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ManageFontsDialogFragment extends AppCompatDialogFragment {
    final static String TAG = "ManageFontsDialog";
    final static int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    protected File DIRECTORY = null;

    String[] defaultFonts;
    ManageFontsDialogListener mListener;
    ListView listView;
    Button btnAddCustomFont;
    FontAdapter arrayAdapter;
    Uri selectedUri;
    boolean isPurchased = false;

    private static void copyFile(FileInputStream src, FileOutputStream dst) throws IOException {
        FileChannel inChannel = src.getChannel();
        FileChannel outChannel = dst.getChannel();

        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }

            outChannel.close();
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

            if (i > 0 && word.length() > 0) {
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

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DIRECTORY = getActivity().getDir("Fonts", Context.MODE_PRIVATE);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogTheme);
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.manage_alarm_sounds_dialog, null);
        listView = view.findViewById(R.id.listView);
        btnAddCustomFont = view.findViewById(R.id.addCustomAlarmTone);

        String btnTxt = getActivity().getString(R.string.add_custom_font);
        if (!isPurchased) {
            String productName = getActivity().getString(R.string.product_name_pro);
            btnTxt += "\n (" + productName + ")";
            Context context = getContext();
            if (context != null) {
                int color = Utility.getRandomMaterialColor(getContext());
                int textColor = Utility.getContrastColor(color);
                btnAddCustomFont.setBackgroundColor(color);
                btnAddCustomFont.setTextColor(textColor);
            }
        }
        btnAddCustomFont.setText(btnTxt);

        btnAddCustomFont.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!isPurchased) {
                    if (mListener != null) {
                        mListener.onPurchaseRequested();
                        dismiss();
                        return;
                    }
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                        && !hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    return;
                }
                selectCustomFont();
            }
        });

        initListView();
        builder.setTitle(R.string.typeface)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (arrayAdapter != null) {
                            if (mListener != null) {
                                FileUri fileUri = arrayAdapter.getSelectedUri();
                                if (fileUri != null) {
                                    mListener.onFontSelected(fileUri.uri, fileUri.name);
                                }
                            }
                            arrayAdapter.release();
                        }
                    }
                })
                .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (arrayAdapter != null) {
                            arrayAdapter.release();
                        }
                    }
                });

        return Utility.createDialogTheme(builder.create());
    }

    private boolean hasPermission(String permission) {
        return Build.VERSION.SDK_INT < 23 ||
                (getActivity().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectCustomFont();
                }
            }
        }
    }

    private void selectCustomFont() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/*");
        startActivityForResult(intent, 1);
    }

    private void initListView() {
        ArrayList<FileUri> staticFiles = getCustomFiles();

        Log.i(TAG, DIRECTORY.getAbsolutePath());
        final File[] file_list = listFiles();
        if (file_list != null && file_list.length > 0) {
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
            File f = new File(file.uri.getPath());
            if (f.exists()) {
                f.delete();
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
                list.add(getFileUri(name, R.string.typeface_roboto_regular));
            }
        }
        return list;
    }

    private FileUri getFileUri(String file, int resId) {
        Uri uri = Uri.parse("file:///android_asset/fonts/" + file);
        return new FileUri(uri, getUserFriendlyFileName(file));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {

            if (resultCode == RESULT_OK) {
                if (DIRECTORY == null) return;
                Uri uri = data.getData();
                ContentResolver contentResolver = getActivity().getContentResolver();
                Cursor returnCursor = contentResolver.query(uri, null, null, null, null);
                if (returnCursor == null) return;

                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();

                String fileName = returnCursor.getString(nameIndex);
                returnCursor.close();

                if (extensionIs(fileName, ".zip")) {
                    extractZipFile(uri);
                } else if (extensionIs(fileName, ".ttf", ".otf")) {
                    copyFileToDirectory(uri, fileName);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void copyFileToDirectory(Uri uri, String fileName) {
        ContentResolver contentResolver = getActivity().getContentResolver();
        File dstFile = new File(DIRECTORY, fileName);
        FileUri uriDst = new FileUri(dstFile);

        boolean isCreated;
        try {
            isCreated = dstFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (!isCreated) {
            return;
        }

        FileInputStream src;
        FileOutputStream dst;
        try {
            src = (FileInputStream) contentResolver.openInputStream(uri);
            dst = (FileOutputStream) contentResolver.openOutputStream(uriDst.uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        try {
            copyFile(src, dst);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        addFileToList(uriDst);
    }

    private boolean extractZipFile(Uri uri) {
        ContentResolver contentResolver = getActivity().getContentResolver();

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
            e.printStackTrace();
            return false;
        }

        return true;
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
