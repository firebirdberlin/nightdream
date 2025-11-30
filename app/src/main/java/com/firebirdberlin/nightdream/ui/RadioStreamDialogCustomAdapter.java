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

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.firebirdberlin.nightdream.R;

import java.util.List;
import java.util.Locale;

public class RadioStreamDialogCustomAdapter extends ArrayAdapter<RadioStreamDialogItem> {

    ImageLoader mImageLoader;
    Context context;

    public RadioStreamDialogCustomAdapter(Context context, int resourceId,
                                          List<RadioStreamDialogItem> items) {
        super(context, resourceId, items);
        this.context = context;
        mImageLoader = VolleySingleton.getInstance().getImageLoader();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        RadioStreamDialogItem item = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.radio_stream_list_adapter, null);
            holder = new ViewHolder();
            holder.txtName = convertView.findViewById(R.id.name);
            holder.txtDesc = convertView.findViewById(R.id.description);
            holder.imageView = convertView.findViewById(R.id.icon);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        holder.txtName.setText(getText(item));
        holder.txtDesc.setText(getDesc(item));

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            holder.imageView.setImageUrl(item.getImageUrl(), mImageLoader);
        }

        holder.imageView.setContentDescription(item.getName());
        holder.imageView.setDefaultImageResId(R.drawable.ic_music_note_24dp);
        holder.imageView.setErrorImageResId(R.drawable.ic_music_note_24dp);

        return convertView;
    }

    public String getText(RadioStreamDialogItem item) {
        String countryCode = (item.getIsCountrySelected()) ? "" : String.format("%s ", item.getCountryCode());
        if (countryCode.equals(" ")) {
            countryCode = "";
        }
        return String.format(Locale.getDefault(), "%s %s",
                countryCode, item.getName());
    }

    public String getDesc(RadioStreamDialogItem item) {
        String streamOffline =
                (item.getIsOnline())
                        ? ""
                        : String.format(" - %s", this.context.getResources().getString(R.string.radio_stream_offline));
        String bitRate =
                (item.getBitrate() == 0) ? "???" : String.format("%s ", item.getBitrate());
        return String.format(Locale.getDefault(), "(%s kbit/s) %s",
                bitRate, streamOffline);
    }

    private class ViewHolder {
        NetworkImageView imageView;
        TextView txtName;
        TextView txtDesc;
    }


}
