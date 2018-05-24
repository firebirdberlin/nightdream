package com.firebirdberlin.nightdream.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.FileUri;

import java.io.IOException;
import java.util.List;


class AlarmToneAdapter extends ArrayAdapter<FileUri> {
    private Context context;
    private int viewId;
    private int selectedPosition = -1;
    private MediaPlayer mediaPlayer;
    private OnDeleteRequestListener listener;

    AlarmToneAdapter(Context context, int viewId, List<FileUri> values) {
        super(context, viewId, R.id.text1, values);
        this.context = context;
        this.viewId = viewId;
        this.mediaPlayer = new MediaPlayer();
    }

    void setOnDeleteRequestListener(OnDeleteRequestListener listener) {
        this.listener = listener;
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        super.getView(position, convertView, parent);
        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        View v = inflater.inflate(viewId, parent, false);
        RadioButton button = (RadioButton) v.findViewById(R.id.text1);
        final FileUri item = getItem(position);
        String name = item.name;
        if (!"content".equals(item.uri.getScheme())) {
            name = Utility.getSoundFileTitleFromUri(context, item.uri);
        }
        button.setText(name);
        button.setChecked(position == selectedPosition);
        button.setTag(position);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean samePosition = selectedPosition == (Integer) view.getTag();
                selectedPosition = (Integer) view.getTag();
                notifyDataSetChanged();

                boolean justStop = (samePosition && mediaPlayer != null && mediaPlayer.isPlaying());
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    releaseMediaplayer();
                }

                if (item == null || justStop || !shallPlaySound()) {
                    return;
                }

                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                try {
                    mediaPlayer.setDataSource(context, item.uri);
                    mediaPlayer.setLooping(false);
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mediaPlayer.start();

            }
        });
        ImageView buttonDelete = (ImageView) v.findViewById(R.id.buttonDelete);

        buttonDelete.setVisibility(
                item != null && "file".equals(item.uri.getScheme()) &&
                        !item.uri.toString().contains("android_asset")
                        ? View.VISIBLE : View.GONE);
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("DIALOG", "delete clicked");
                if (item == null) {
                    return;
                }
                if (listener != null) {
                    listener.onDeleteRequested(item);
                }

                FileUri selected = getSelectedUri();
                remove(item);
                notifyDataSetChanged();
                if (selected != null) {
                    setSelectedUri(selected.uri);
                }
                releaseMediaplayer();

            }

        });

        // click effect
        buttonDelete.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        ImageView view = (ImageView) v;
                        //overlay is black with transparency of 0x77 (119)
                        view.getDrawable().setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                        view.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        ImageView view = (ImageView) v;
                        //clear the overlay
                        view.getDrawable().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                }

                return false;
            }
        });

        return v;
    }

    private boolean shallPlaySound() {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        switch (audio.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:
                return true;
            case AudioManager.RINGER_MODE_SILENT:
                return false;
            case AudioManager.RINGER_MODE_VIBRATE:
                return false;
        }
        return false;
    }

    private void releaseMediaplayer() {
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.reset();
        mediaPlayer.release();
        mediaPlayer = null;
    }

    public void release() {
        releaseMediaplayer();
    }

    public FileUri getSelectedUri() {
        if (selectedPosition < 0 || selectedPosition >= getCount()) {
            return null;
        }

        FileUri item = getItem(selectedPosition);
        if (item != null ) {
            return item;
        }
        return null;
    }

    public void setSelectedUri(Uri uri) {
        if (uri == null) return;
        for (int i = 0; i < getCount(); i++){
            FileUri item = getItem(i);
            if (uri.equals(item.uri)) {
                selectedPosition = i;
                notifyDataSetChanged();
                break;
            }
        }
    }

    interface OnDeleteRequestListener {
        void onDeleteRequested(FileUri file);
    }
}
