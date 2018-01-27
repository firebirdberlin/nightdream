package com.firebirdberlin.nightdream.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
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

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.models.FileUri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ManageFontsDialogFragment extends DialogFragment {
    final static String TAG = "ManageFontsDialog";
    final static int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    protected File DIRECTORY = null;
    ManageFontsDialogListener mListener;
    ListView listView;
    Button addCustomAlarmTone;
    AlarmToneAdapter arrayAdapter;
    Uri selectedUri;

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
        listView = (ListView) view.findViewById(R.id.listView);
        addCustomAlarmTone = (Button) view.findViewById(R.id.addCustomAlarmTone);
        addCustomAlarmTone.setText(getActivity().getString(R.string.add_custom_font));

        addCustomAlarmTone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
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
        return builder.create();
    }

    private boolean hasPermission(String permission) {
        return Build.VERSION.SDK_INT < 23 ||
                (getActivity().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
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
        ArrayList<FileUri> sounds = getCustomFiles();

        Log.i(TAG, DIRECTORY.getAbsolutePath());
        final File file_list[] = listFiles();
        if (file_list != null && file_list.length > 0) {
            for (File file : file_list) {
                sounds.add(new FileUri(file));
            }

        }
        arrayAdapter = new AlarmToneAdapter(getActivity(), R.layout.list_item_alarm_tone, sounds);
        arrayAdapter.setSelectedUri(selectedUri);
        arrayAdapter.setOnDeleteRequestListener(new AlarmToneAdapter.OnDeleteRequestListener() {
            @Override
            public void onDeleteRequested(FileUri file) {
                if (file == null || file.uri == null) {
                    return;
                }
                File f = new File(file.uri.getPath());
                if (f.exists()) {
                    f.delete();
                }
            }
        });
        listView.setAdapter(arrayAdapter);
    }

    public ArrayList<FileUri> getCustomFiles() {
        ArrayList<FileUri> list = new ArrayList<>();
        list.add(getFileUri("roboto_regular.ttf", R.string.typeface_roboto_regular));
        list.add(getFileUri("roboto_light.ttf", R.string.typeface_roboto_light));
        list.add(getFileUri("roboto_condensed.ttf", R.string.typeface_roboto_condensed));
        list.add(getFileUri("roboto_thin.ttf", R.string.typeface_roboto_thin));
        list.add(getFileUri("roboto_medium.ttf", R.string.typeface_roboto_medium));
        list.add(getFileUri("7segment.ttf", R.string.typeface_7_segment));

//        list.add(new FileUri( Uri.parse("file:///android_asset/fonts/roboto_regular.ttf"), getContext().getString(R.string.typeface_roboto_medium)))
//        try {
//            String[] assets = getActivity().getAssets().list("fonts");
//            for (String file : assets) {
//                if ""
//                Uri uri = Uri.parse("file:///android_asset/fonts/" + file);
//                list.add(new FileUri(uri, file));
//            }
//        } catch (IOException e) {
//
//        }
        return list;
    }

    private FileUri getFileUri(String file, int resId) {
        Uri uri = Uri.parse("file:///android_asset/fonts/" + file);
        return new FileUri(uri, getActivity().getString(resId));
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
                /*
                 * Get the column indexes of the data in the Cursor,
                 * move to the first row in the Cursor, get the data,
                 * and display it.
                 */
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                //int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                returnCursor.moveToFirst();

                String fileName = returnCursor.getString(nameIndex);
                Log.w(TAG, fileName);
                String mimeType = contentResolver.getType(uri);
                Log.w(TAG, mimeType);
                //long fileSize = returnCursor.getLong(sizeIndex);
                returnCursor.close();
                File dstFile = new File(DIRECTORY, fileName);
                FileUri uriDst = new FileUri(dstFile);

                boolean isCreated = false;
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

                Log.i(TAG, uri.getPath() + " => " + dstFile.getAbsolutePath());
                Log.i(TAG, uriDst.name + "/" + uriDst.uri);

                try {
                    copyFile(src, dst);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                if (arrayAdapter != null) {
                    arrayAdapter.add(uriDst);
                    arrayAdapter.notifyDataSetChanged();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setOnFontSelectedListener(ManageFontsDialogListener listener) {
        this.mListener = listener;
    }

    public interface ManageFontsDialogListener {
        void onFontSelected(Uri uri, String name);
    }
}
