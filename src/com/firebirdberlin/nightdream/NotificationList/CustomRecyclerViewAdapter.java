package com.firebirdberlin.nightdream.NotificationList;

import android.app.PendingIntent;
import android.content.Context;
import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.firebirdberlin.nightdream.R;

import java.util.List;

public class CustomRecyclerViewAdapter extends RecyclerView.Adapter<NotificationViewHolder> {
    public static String TAG = "CustomRecyclerViewAdapter";

    private List<Notification> notificationlist;
    private Context context;

    public CustomRecyclerViewAdapter(Context context, List<Notification> datas ) {
        this.context = context;
        this.notificationlist = datas;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        Log.d(TAG,"BrowseNotification started");

        // Inflate view from notify_item_layout.xml
        final View recyclerViewItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_list_item_view, parent, false);

        //Interface definition for a callback to be invoked when a view is clicked.
        recyclerViewItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    handleRecyclerItemClick((RecyclerView) parent, v);
                }catch(Exception ex) {
                    Log.e(TAG, "setOnClickListener", ex);
                }
            }
        });

        return new NotificationViewHolder(recyclerViewItem);
    }

    //to update the Data
    public void updateData(List<Notification> viewnotificationlist) {
        notificationlist = viewnotificationlist;

        //Notifies the attached observers that the underlying data has been changed and any View reflecting the data set should refresh itself.
        notifyDataSetChanged();
    }

    //Called by RecyclerView to display the data at the specified position.
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onBindViewHolder(NotificationViewHolder holder, int position) {

        holder.itemView.setTag(""+position);

        // get notification in notificationlist via position
        final Notification notify = this.notificationlist.get(position);

        // Bind data to viewholder
        holder.notificationLargePicture.setVisibility(View.GONE);

        //Layout without style / Remoteview supported by Notification?

        holder.notificationRemoteView.removeAllViews();
        holder.itemView.findViewById(R.id.notify).setVisibility(View.VISIBLE);

        holder.notificationMessagebitmap.setImageDrawable(notify.get_notification_drawableicon());

        if (notify.get_notification_color() != 0) {
            holder.notificationMessagebitmap.setColorFilter(notify.get_notification_color(), PorterDuff.Mode.SRC_ATOP);
        }else {
            holder.notificationMessagebitmap.setColorFilter(Color.parseColor("#000000"), PorterDuff.Mode.SRC_ATOP);
        }

        if (notify.get_notification_bitmaplargeicon() != null){
            holder.notificationMessageLargeIcon.setVisibility(View.VISIBLE);
            holder.notificationMessageLargeIcon.setImageBitmap(notify.get_notification_bitmaplargeicon());
        }
        else{
            holder.notificationMessageLargeIcon.setVisibility(View.GONE);
        }

        holder.notificationAppname.setText(notify.get_notification_applicationname() );
        holder.notificationpostTimeView.setText(notify.get_notification_posttime() );
        holder.notificationTitle.setText(notify.get_notification_title());

        holder.notificationText.setText(notify.get_notification_text());

        //get actions
        for(TextView ActionText: holder.notificationActionText)
        {
            ActionText.setVisibility(View.GONE);
        }

        try {
            int positionAction = 0;

            if (notify.get_notification_action() != null) {
                for (final android.app.Notification.Action action : notify.get_notification_action()) {
                    try {
                        Log.d(TAG, "ShowActionText ("+positionAction+"): " + action.title.toString().toUpperCase());
                        holder.notificationActionText[positionAction].setVisibility(View.VISIBLE);
                        holder.notificationActionText[positionAction].setText(action.title.toString().toUpperCase());
                        holder.notificationActionText[positionAction].setTextColor(Color.rgb(0, 0, 200));

                        holder.notificationActionText[positionAction].setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                try {
                                    action.actionIntent.send();
                                } catch (Exception ex) {
                                    Log.e(TAG,ex.toString());
                                }
                            }
                        });
                        positionAction++;
                    } catch (Exception ex) {
                        Log.e(TAG, "ShowActionText: "+ex );
                        for (TextView ActionText : holder.notificationActionText) {
                            ActionText.setVisibility(View.GONE);
                        }
                    }
                }//end for
            }
        }catch (Exception ex){
            Log.e(TAG,ex.toString());
        }
        //End Actions

        //get template style
        String[] template = {"BigPictureStyle", "BigTextStyle", "DecoratedCustomViewStyle", "InboxStyle", "MessagingStyle"};

        String match = "default";
        for (String s : template) {
            if (notify.get_notification_template().contains(s)) {
                match = s;
                break;
            }
        }

        switch (match) {
            case "DecoratedCustomViewStyle":
                try{
                    if(notify.get_notification_bigview().getParent() != null) {
                        ((ViewGroup)notify.get_notification_bigview().getParent()).removeView(notify.get_notification_bigview()); // <- fix
                    }
                    holder.notificationRemoteView.removeAllViews();
                    holder.itemView.findViewById(R.id.notify).setVisibility(View.GONE);
                    if (holder.notificationRemoteView.getChildCount() == 0) {
                        holder.notificationRemoteView.addView(notify.get_notification_bigview());
                    }
                }catch (Exception ex){
                   Log.e(TAG, "DecoratedCustomViewStyle", ex);
                }
                break;
            case "InboxStyle":
                holder.notificationText.setText(notify.get_notification_textlines());
                break;
            case "MessagingStyle":
                holder.notificationText.setText(notify.get_notification_messages());
                break;
            case "BigTextStyle":
                if (!notify.get_notification_titlebig().equals("")){
                    holder.notificationTitle.setText(notify.get_notification_titlebig());
                }
                else{
                    holder.notificationTitle.setText(notify.get_notification_title());
                }
                if (!notify.get_notification_textbig().equals("")){
                    holder.notificationText.setText(notify.get_notification_textbig());
                }
                else{
                    holder.notificationText.setText(notify.get_notification_text());
                }
                break;
            case "BigPictureStyle":
                if (!notify.get_notification_titlebig().equals("")){
                    holder.notificationTitle.setText(notify.get_notification_titlebig());
                }
                else{
                    holder.notificationTitle.setText(notify.get_notification_title());
                }
                if (!notify.get_notification_summarytext().equals("")){
                    holder.notificationText.setText(notify.get_notification_summarytext());
                }
                else{
                    holder.notificationText.setText(notify.get_notification_text());
                }

                if (notify.get_notification_bitmaplargeicon() == null){
                    holder.notificationMessageLargeIcon.setVisibility(View.VISIBLE);
                    holder.notificationMessageLargeIcon.setImageBitmap(notify.get_notification_bigpicture());
                }

                holder.notificationLargePicture.setVisibility(View.VISIBLE);
                holder.notificationLargePicture.setImageBitmap(notify.get_notification_bigpicture());
                holder.notificationLargePicture.bringToFront();
                break;
            case "default":
                //try to set a custom view without style
                holder.itemView.findViewById(R.id.notify_remoteview);

                for(int index = 0; index < ((ViewGroup) holder.notificationRemoteView).getChildCount(); index++) {
                    View nextChild = ((ViewGroup) holder.notificationRemoteView).getChildAt(index);
                    holder.notificationRemoteView.removeView(nextChild);
                }

                try{
                    if(notify.get_notification_view()!=null) {
                        if (notify.get_notification_view().getParent() != null) {
                            ((ViewGroup) notify.get_notification_view().getParent()).removeView(notify.get_notification_view());
                        }
                        holder.notificationRemoteView.removeAllViews();
                        holder.notificationRemoteView.addView(notify.get_notification_view());

                        holder.itemView.findViewById(R.id.notify).setVisibility(View.GONE);
                    }
                }catch (Exception ex) {
                    Log.e(TAG, "defaultstyle: ", ex);
                }
                break;
        }//End of switch
    }

    //Returns the total number of items in the data set held by the adapter.
    @Override
    public int getItemCount() {
        return this.notificationlist.size();
    }

    // Interface definition for a callback to be invoked when a view is clicked.
    private void handleRecyclerItemClick(RecyclerView recyclerView, View itemView) throws PendingIntent.CanceledException, IntentSender.SendIntentException {
        int itemPosition = recyclerView.getChildLayoutPosition(itemView);
        Notification notify  = this.notificationlist.get(itemPosition);
        try {
            if (notify.get_notification_contentintent() != null) {
                notify.get_notification_contentintent().send();
            }
        } catch (PendingIntent.CanceledException e) {
            throw new IntentSender.SendIntentException(e);
        }
    }

}