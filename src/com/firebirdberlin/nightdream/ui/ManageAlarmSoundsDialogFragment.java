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
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.FileUri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class ManageAlarmSoundsDialogFragment extends AppCompatDialogFragment {
    final static String TAG = "ManageAlarmSoundsDialog";
    private File DIRECTORY = null;
    private ManageAlarmSoundsDialogListener mListener;
    private ListView listView;
    private AlarmToneAdapter arrayAdapter;
    private Uri selectedUri;
    private boolean isPurchased = false;
    private Context context;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted
                    Log.d(TAG, "ActivityResultLauncher Permission granted");
                    selectCustomTone();
                } else {
                    // Permission is denied, feature is unavailable
                    Log.d(TAG, "ActivityResultLauncher already been shown and permission is denied");
                    Toast.makeText(
                            context,
                            context.getString(R.string.permission_request_storage),
                            Toast.LENGTH_LONG
                    ).show();
                }
            });

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

    public void setIsPurchased(boolean isPurchased) {
        this.isPurchased = isPurchased;
    }

    private File[] listFiles() {
        if (DIRECTORY == null) return null;
        return DIRECTORY.listFiles();
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setSelectedUri(String uriString) {
        selectedUri = (uriString != null) ? Uri.parse(uriString) : null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DIRECTORY = getActivity().getDir("Alarms", Context.MODE_PRIVATE);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogTheme);
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.manage_alarm_sounds_dialog, null);
        listView = view.findViewById(R.id.listView);
        Button addCustomAlarmTone = view.findViewById(R.id.addCustomAlarmTone);
        String btnTxt = getActivity().getString(R.string.add_custom_alarm_tone);
        if (!isPurchased) {
            String productName = getActivity().getString(R.string.product_name_webradio);
            btnTxt += "\n (" + productName + ")";
            int color = Utility.getRandomMaterialColor(context);
            int textColor = Utility.getContrastColor(color);
            addCustomAlarmTone.setBackgroundColor(color);
            addCustomAlarmTone.setTextColor(textColor);
        }
        addCustomAlarmTone.setText(btnTxt);
        addCustomAlarmTone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!isPurchased) {
                    if (mListener != null) {
                        mListener.onPurchaseRequested();
                        dismiss();
                        return;
                    }
                }

                String permission =
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                ? Manifest.permission.READ_MEDIA_AUDIO
                                : Manifest.permission.READ_EXTERNAL_STORAGE;
                if (!hasPermission(permission)) {
                    requestPermissionLauncher.launch(permission);
                    return;
                }
                selectCustomTone();
            }
        });

        initListView();
        builder.setTitle(R.string.choose_alarm_sound)
                .setIcon(R.drawable.ic_alarm_clock)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (arrayAdapter != null) {
                            if (mListener != null) {
                                FileUri fileUri = arrayAdapter.getSelectedUri();
                                if (fileUri != null) {
                                    Settings.setDefaultAlarmTone(context, fileUri.uri.toString());
                                    mListener.onAlarmToneSelected(fileUri.uri, fileUri.name);
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
        Dialog dialog = (Dialog) builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.border_dialog);
        return dialog;
    }

    private boolean hasPermission(String permission) {
        return Build.VERSION.SDK_INT < 23 ||
                (getActivity().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
    }

    private void selectCustomTone() {
        Intent intent = new Intent();
        intent.setType("audio/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 1);
    }

    private void initListView() {
        ArrayList<FileUri> sounds = getAlarmSounds();

        final File[] file_list = listFiles();
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
        // scroll to the position
        listView.setSelection(arrayAdapter.getSelectedPosition());
    }

    public ArrayList<FileUri> getAlarmSounds() {
        RingtoneManager manager = new RingtoneManager(getActivity());
        manager.setType(RingtoneManager.TYPE_ALARM);
        Cursor cursor = manager.getCursor();
        ArrayList<FileUri> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            String id = cursor.getString(RingtoneManager.ID_COLUMN_INDEX);
            String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
            String uriString = cursor.getString(RingtoneManager.URI_COLUMN_INDEX);
            Uri uri = Uri.parse(uriString + "/" + id);
            list.add(new FileUri(uri, title));
        }

        return list;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {

            if (resultCode == RESULT_OK) {
                if (DIRECTORY == null) return;
                //the selected audio.
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
                //String mimeType = contentResolver.getType(uri);
                //long fileSize = returnCursor.getLong(sizeIndex);
                returnCursor.close();
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

                if (arrayAdapter != null) {
                    arrayAdapter.add(uriDst);
                    arrayAdapter.setSelectedUri(uriDst.uri);
                    // scroll to the position
                    listView.setSelection(arrayAdapter.getSelectedPosition());
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void setOnAlarmToneSelectedListener(ManageAlarmSoundsDialogListener listener) {
        this.mListener = listener;
    }

    public interface ManageAlarmSoundsDialogListener {
        void onAlarmToneSelected(Uri uri, String name);

        void onPurchaseRequested();
    }
}