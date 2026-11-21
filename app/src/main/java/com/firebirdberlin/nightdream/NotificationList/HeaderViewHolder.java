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

package com.firebirdberlin.nightdream.NotificationList;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebirdberlin.nightdream.R;

public class HeaderViewHolder extends RecyclerView.ViewHolder {

    ImageView headerIcon;
    TextView headerAppname;
    TextView headerCount;
    TextView headerAction;

    public HeaderViewHolder(@NonNull View itemView) {
        super(itemView);
        this.headerIcon = itemView.findViewById(R.id.header_icon);
        this.headerAppname = itemView.findViewById(R.id.header_appname);
        this.headerCount = itemView.findViewById(R.id.header_count);
        this.headerAction = itemView.findViewById(R.id.header_action);
    }
}
