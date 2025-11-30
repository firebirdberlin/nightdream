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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.models.FileUri;
import com.firebirdberlin.nightdream.models.FontCache;

import java.util.List;


class FontAdapter extends ArrayAdapter<FileUri> {
    public static final String TAG = "FontAdapter";
    private Context context = null;
    private int viewId = -1;
    private int selectedPosition = -1;
    private OnDeleteRequestListener listener;

    FontAdapter(Context context, int viewId, List<FileUri> values) {
        super(context, viewId, R.id.text1, values);
        this.context = context;
        this.viewId = viewId;
    }

    void setOnDeleteRequestListener(OnDeleteRequestListener listener) {
        this.listener = listener;
    }

    // class for holding the cached view
    static class ViewHolder {
        RadioButton button;
        ImageView buttonDelete;
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Log.d(TAG, "getView()");
        super.getView(position, convertView, parent);

        // holder of the views
        ViewHolder viewHolder;

        final FileUri item = getItem(position);

        if (convertView == null) {
            Log.d(TAG, "getView() convertView == null");
            // create the container ViewHolder
            viewHolder = new ViewHolder();

            // inflate the views from layout for the new row
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(viewId, parent, false);
            viewHolder.button = convertView.findViewById(R.id.text1);
            viewHolder.buttonDelete = convertView.findViewById(R.id.buttonDelete);
            // save the viewHolder to be reused later.
            convertView.setTag(viewHolder);
        }else {
            // there is already ViewHolder, reuse it.
            Log.d(TAG, "getView() convertView");
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Typeface typeface = loadTypefaceForItem(item);
        viewHolder.button.setTypeface(typeface);
        viewHolder.button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        viewHolder.button.setText(item != null ? item.name : "");
        viewHolder.button.setChecked(position == selectedPosition);
        viewHolder.button.setTag(position);
        viewHolder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedPosition = (Integer) view.getTag();
                notifyDataSetChanged();
            }
        });


        viewHolder.buttonDelete.setVisibility(
                item != null && "file".equals(item.uri.getScheme()) &&
                        !item.uri.toString().contains("android_asset")
                        ? View.VISIBLE : View.GONE);
        viewHolder.buttonDelete.setOnClickListener(new View.OnClickListener() {
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
                setSelectedUri(selected.uri);
            }
        });

        // click effect
        viewHolder.buttonDelete.setOnTouchListener(new View.OnTouchListener() {
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

        return convertView;
    }


    private Typeface loadTypefaceForItem(FileUri item) {
        return FontCache.get(context, item.uri.toString());
    }

    public void release() {

    }

    public FileUri getSelectedUri() {
        if (selectedPosition < 0 || selectedPosition >= getCount()) {
            return null;
        }

        return getItem(selectedPosition);
    }

    public void setSelectedUri(Uri uri) {
        if (uri == null) return;
        for (int i = 0; i < getCount(); i++) {
            FileUri item = getItem(i);
            if (uri.equals(item.uri)) {
                selectedPosition = i;
                notifyDataSetChanged();
                return;
            }
        }
        if (getCount() > 0) {
            selectedPosition = 0;
            notifyDataSetChanged();
        }
    }

    interface OnDeleteRequestListener {
        void onDeleteRequested(FileUri file);
    }
}
