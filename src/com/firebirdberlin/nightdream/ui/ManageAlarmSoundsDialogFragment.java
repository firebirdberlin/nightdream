package com.firebirdberlin.nightdream.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
public class ManageAlarmSoundsDialogFragment extends DialogFragment {
    final static String TAG = "ManageAlarmSoundsDialog";
    protected File DIRECTORY = null;
    // Use this instance of the interface to deliver action events
    ManageAlarmSoundsDialogListener mListener;
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

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ManageAlarmSoundsDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ManageAlarmSoundsDialogListener");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (ManageAlarmSoundsDialogListener) context;

        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement ManageAlarmSoundsDialogListener");
        }
    }

    public void setSelectedUri(String uriString) {
        selectedUri = Uri.parse(uriString);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DIRECTORY = getActivity().getDir("Alarms", Context.MODE_PRIVATE);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogTheme);
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.manage_alarm_sounds_dialog, null);
        listView = (ListView) view.findViewById(R.id.listView);
        addCustomAlarmTone = (Button) view.findViewById(R.id.addCustomAlarmTone);

        addCustomAlarmTone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("audio/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, 1);
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
                            mListener.onAlarmToneSelected(arrayAdapter.getSelectedUri());
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

    private void initListView() {
        ArrayList<FileUri> sounds = getAlarmSounds();

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
                Log.i(TAG, uri.getPath());

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

                try {
                    dstFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
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

    public interface ManageAlarmSoundsDialogListener {
        void onAlarmToneSelected(Uri uri);
    }
}