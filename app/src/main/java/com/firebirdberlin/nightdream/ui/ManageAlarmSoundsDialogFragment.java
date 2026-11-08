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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.FileUri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class ManageAlarmSoundsDialogFragment extends AppCompatDialogFragment {
    private static final String TAG = ManageAlarmSoundsDialogFragment.class.getSimpleName();
    private File DIRECTORY = null;
    private ManageAlarmSoundsDialogListener mListener;
    private ListView listView;
    private AlarmToneAdapter arrayAdapter;
    private Uri selectedUri;
    private boolean isPurchased = false;
    private Context context;

    private Context getResolvedContext() {
        if (context != null) {
            return context;
        }
        Context ctx = getContext();
        if (ctx != null) {
            return ctx;
        }
        return getActivity();
    }

    private final ActivityResultLauncher<Intent> selectToneLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (DIRECTORY == null) {
                        return;
                    }
                    Uri uri = data.getData();
                    if (uri == null) {
                        return;
                    }

                    Context ctx = getResolvedContext();
                    if (ctx == null) {
                        return;
                    }

                    ContentResolver contentResolver = ctx.getContentResolver();
                    try (Cursor returnCursor = contentResolver.query(uri, null, null, null, null)) {
                        if (returnCursor == null) {
                            return;
                        }

                        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex == -1 || !returnCursor.moveToFirst()) {
                            return;
                        }

                        String fileName = returnCursor.getString(nameIndex);
                        if (Utility.isEmpty(fileName)) {
                            return;
                        }
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

                        if (arrayAdapter != null) {
                            arrayAdapter.add(uriDst);
                            arrayAdapter.setSelectedUri(uriDst.uri);
                            // scroll to the position
                            listView.setSelection(arrayAdapter.getSelectedPosition());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing selected alarm sound file", e);
                    }
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                Context ctx = getResolvedContext();
                if (isGranted) {
                    // Permission is granted
                    Log.d(TAG, "ActivityResultLauncher Permission granted");
                    selectCustomTone();
                } else {
                    // Permission is denied, feature is unavailable
                    Log.d(TAG, "ActivityResultLauncher already been shown and permission is denied");
                    if (ctx != null) {
                        Toast.makeText(
                                ctx,
                                ctx.getString(R.string.permission_request_storage),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
            });

    private static void copyFile(FileInputStream src, FileOutputStream dst) throws IOException {
        try (FileChannel inChannel = src.getChannel(); FileChannel outChannel = dst.getChannel()) {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
    }

    public void setIsPurchased(boolean isPurchased) {
        this.isPurchased = isPurchased;
    }

    private File[] listFiles() {
        if (DIRECTORY == null) {
            return null;
        }
        return DIRECTORY.listFiles();
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setSelectedUri(String uriString) {
        selectedUri = (uriString != null) ? Uri.parse(uriString) : null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context ctx = getResolvedContext();
        if (ctx == null) {
            return super.onCreateDialog(savedInstanceState);
        }
        DIRECTORY = ctx.getDir("Alarms", Context.MODE_PRIVATE);

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx, R.style.DialogTheme);
        LayoutInflater inflater = LayoutInflater.from(ctx);

        View view = inflater.inflate(R.layout.manage_alarm_sounds_dialog, null);
        listView = view.findViewById(R.id.listView);
        Button addCustomAlarmTone = view.findViewById(R.id.addCustomAlarmTone);
        String btnTxt = ctx.getString(R.string.add_custom_alarm_tone);
        if (!isPurchased) {
            String productName = ctx.getString(R.string.product_name_pro);
            btnTxt += "\n (" + productName + ")";
            int color = Utility.getRandomMaterialColor(ctx);
            int textColor = Utility.getContrastColor(color);
            addCustomAlarmTone.setBackgroundColor(color);
            addCustomAlarmTone.setTextColor(textColor);
        }
        addCustomAlarmTone.setText(btnTxt);
        addCustomAlarmTone.setOnClickListener(view1 -> {

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
        });

        initListView();
        builder.setTitle(R.string.choose_alarm_sound)
                .setIcon(R.drawable.ic_alarm_clock)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    if (arrayAdapter != null) {
                        if (mListener != null) {
                            FileUri fileUri = arrayAdapter.getSelectedUri();
                            if (fileUri != null && fileUri.uri != null) {
                                Settings.setDefaultAlarmTone(ctx, fileUri.uri.toString());
                                mListener.onAlarmToneSelected(fileUri.uri, fileUri.name);
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
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.border_dialog);
        }
        return dialog;
    }

    private boolean hasPermission(String permission) {
        Context ctx = getResolvedContext();
        if (ctx == null) {
            return false;
        }
        return ctx.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void selectCustomTone() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        selectToneLauncher.launch(intent);
    }

    private void initListView() {
        Context ctx = getResolvedContext();
        if (ctx == null || listView == null) {
            return;
        }

        ArrayList<FileUri> sounds = getAlarmSounds();

        final File[] file_list = listFiles();
        if (file_list != null) {
            for (File file : file_list) {
                if (file != null) {
                    sounds.add(new FileUri(file));
                }
            }
        }
        arrayAdapter = new AlarmToneAdapter(ctx, R.layout.list_item_alarm_tone, sounds);
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
        // scroll to the position
        listView.setSelection(arrayAdapter.getSelectedPosition());
    }

    public ArrayList<FileUri> getAlarmSounds() {
        Context ctx = getResolvedContext();
        ArrayList<FileUri> list = new ArrayList<>();
        if (ctx == null) {
            return list;
        }
        RingtoneManager manager = new RingtoneManager(ctx);
        manager.setType(RingtoneManager.TYPE_ALARM);
        try (Cursor cursor = manager.getCursor()) {
            if (cursor == null) {
                return list;
            }
            while (cursor.moveToNext()) {
                String id = cursor.getString(RingtoneManager.ID_COLUMN_INDEX);
                String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
                String uriString = cursor.getString(RingtoneManager.URI_COLUMN_INDEX);
                if (uriString != null && id != null) {
                    Uri uri = Uri.parse(uriString + "/" + id);
                    list.add(new FileUri(uri, title));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting alarm sounds", e);
        }

        return list;
    }

    void setOnAlarmToneSelectedListener(ManageAlarmSoundsDialogListener listener) {
        this.mListener = listener;
    }

    public interface ManageAlarmSoundsDialogListener {
        void onAlarmToneSelected(Uri uri, String name);

        void onPurchaseRequested();
    }
}
