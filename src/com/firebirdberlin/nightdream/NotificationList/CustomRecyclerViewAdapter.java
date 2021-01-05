package com.firebirdberlin.nightdream.NotificationList;


import android.app.PendingIntent;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebirdberlin.nightdream.NotificationList.Notification;
import com.firebirdberlin.nightdream.R;

import java.util.ArrayList;
import java.util.List;

public class CustomRecyclerViewAdapter extends RecyclerView.Adapter<NotificationViewHolder> {
    public static String TAG = "CustomRecyclerViewAdapter";
    private SharedPreferences sharedPreferences;
    private boolean started = false;
    private Handler handler = new Handler();

    private List<Notification> notificationlist;
    private Context context;
    private Boolean notifyinformation;

    private ArrayList<checked> selected;

    public CustomRecyclerViewAdapter(Context context, List<Notification> datas ) {
        this.context = context;
        this.notificationlist = datas;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        //Toast.makeText(this.context, "oncrv", Toast.LENGTH_SHORT).show();
        Log.d(TAG,"BrowseNotification started");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        notifyinformation = sharedPreferences.getBoolean("notifyinformation", true);

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

        recyclerViewItem.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                // String itemviewtag = (String) view.getTag();

                int itemviewtag = Integer.parseInt((String) view.getTag());

                // selected.get((int) itemviewtag).isChecked();

                ColorDrawable viewColor = (ColorDrawable) recyclerViewItem.findViewById(R.id.cardViewLayout).getBackground();;
                int colorId = viewColor.getColor();
                String hexColor = String.format("#%06X", (0xFFFFFF & colorId));

                // Toast.makeText(context, "Long Click - " + selected.size(), Toast.LENGTH_SHORT).show();

                if (selected.get(itemviewtag).isChecked()) {
                    recyclerViewItem.findViewById(R.id.cardViewLayout).setBackgroundColor(0xFFFFFFFF);
                }
                else{
                    recyclerViewItem.findViewById(R.id.cardViewLayout).setBackgroundColor(0xFFFF0000);
                }
                selected.get(itemviewtag).setChecked(!selected.get(itemviewtag).isChecked());

                return true;
            }
        });

        return new NotificationViewHolder(recyclerViewItem);
    }

    //to update the Data
    public void updateData(List<Notification> viewnotificationlist) {
        //notificationlist.clear();
        notificationlist = viewnotificationlist;

        //Notifies the attached observers that the underlying data has been changed and any View reflecting the data set should refresh itself.
        notifyDataSetChanged();
    }

    //Called by RecyclerView to display the data at the specified position.
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onBindViewHolder(NotificationViewHolder holder, int position) {

        holder.itemView.setTag(""+position);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        notifyinformation = sharedPreferences.getBoolean("notifyinformation", true);

        // if(Const.DEBUG) Log.d(Const.DEBUG_TAG,"notifyinformation= " + notifyinformation);

        if (!selected.get(position).isChecked()) {
            holder.itemView.findViewById(R.id.cardViewLayout).setBackgroundColor(0xFFFFFFFF);
        }
        else{
            holder.itemView.findViewById(R.id.cardViewLayout).setBackgroundColor(0xFFFF0000);
        }


        //show more informations from notification
        if (notifyinformation)
        {
            holder.itemView.findViewById(R.id.notify_informations).setVisibility(View.VISIBLE);
        }
        else{
            holder.itemView.findViewById(R.id.notify_informations).setVisibility(View.GONE);
        }

        // get notification in notificationlist via position
        final Notification notify = this.notificationlist.get(position);

        // Bind data to viewholder
        holder.itemView.findViewById(R.id.notify_actions_images).setVisibility(View.GONE);
        //holder.notificationAction.setVisibility(View.GONE);
        holder.notificationLargePicture.setVisibility(View.GONE);
        holder.itemView.findViewById(R.id.notify_background).setBackground(null);

        //Layout without style / Remoteview supported by Notification?

        holder.notificationRemoteView.removeAllViews();
        holder.itemView.findViewById(R.id.notify).setVisibility(View.VISIBLE);

        holder.notificationMessagebitmap.setImageDrawable(notify.get_notification_drawableicon());
        holder.notificationMessagebitmap.setColorFilter(Color.parseColor("#000000"), PorterDuff.Mode.SRC_ATOP);

        // Toast.makeText(context, "LargeIcon: "+notify.get_notification_bitmaplargeicon(), Toast.LENGTH_SHORT).show();

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
        for( TextView ActionText: holder.notificationActionText )
        {
            ActionText.setVisibility(View.GONE);
        }

        try {
            int positionAction = 0;

            if (notify.get_notification_action() != null) {
                for (final android.app.Notification.Action action : notify.get_notification_action()) {
                    try {
                        //No picture just text
                        holder.itemView.findViewById(R.id.notify_actions_images).setVisibility(View.GONE);
                        holder.notificationActionText[positionAction].setVisibility(View.VISIBLE);
                        holder.notificationActionText[positionAction].setText(action.title.toString().toUpperCase());
                        holder.notificationActionText[positionAction].setTextColor(Color.rgb(0, 0, 200));
                        Log.i(TAG, "Show Action: " + action.title.toString().toUpperCase());
                        //Toast.makeText(context, action.title.toString().toUpperCase(), Toast.LENGTH_SHORT).show();

                        holder.notificationActionText[positionAction].setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                // Toast.makeText(context, "onClick Action", Toast.LENGTH_SHORT).show();
                                try {
                                    action.actionIntent.send();
                                } catch (Exception ex) {
                                    Log.e(TAG,ex.toString());
                                }
                            }
                        });
                        positionAction++;
                    } catch (Exception exBigTextStyle) {
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
        String[] template = {"BigPictureStyle", "BigTextStyle", "DecoratedCustomViewStyle", "InboxStyle", "MediaStyle", "MessagingStyle"};

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
                    //Toast.makeText(this.context, "oncrv - " + holder.notificationRemoteView.getChildCount(), Toast.LENGTH_SHORT).show();
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
            case "MediaStyle":
                for( ImageView ActionImage: holder.notificationActionImages )
                {
                    ActionImage.setVisibility(View.GONE);
                }

                for( TextView ActionText: holder.notificationActionText )
                {
                    ActionText.setVisibility(View.GONE);
                }

                try {
                    int positionAction = 0;
                    for (final android.app.Notification.Action action : notify.get_notification_action()) {
                        //Toast.makeText(context, "in Dynamic"+action.title, Toast.LENGTH_SHORT).show();
                        try {
                            //holder.notificationAction.setVisibility(View.GONE);
                            holder.notificationMessageLargeIcon.setVisibility(View.GONE);
                            holder.itemView.findViewById(R.id.notify_actions_images).setVisibility(View.VISIBLE);
                            holder.itemView.findViewById(R.id.notify_background).setBackground(new BitmapDrawable(context.getResources(), notify.get_notification_bitmaplargeicon()));

                            Context remotePackageContext = context.getApplicationContext().createPackageContext((String) notify.get_notification_package(), 0);
                            Drawable notification_drawableicon = ContextCompat.getDrawable(remotePackageContext, (int) action.getIcon().getResId());
                            holder.notificationActionImages[positionAction].setImageDrawable(notification_drawableicon);
                            holder.notificationActionImages[positionAction].setVisibility(View.VISIBLE);
                            holder.notificationActionImages[positionAction].setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    // Toast.makeText(context, "onClick Action", Toast.LENGTH_SHORT).show();
                                    try {
                                        action.actionIntent.send();
                                    } catch (Exception ex) {
                                        Log.e(TAG, ex.toString());
                                    }
                                }
                            });

                            positionAction++;
                        } catch (Exception exMediaStyle) {
                            Log.e(TAG, "MediaStyle - Actions: ", exMediaStyle);
                        }
                    }
                }catch (Exception exMediaStyle){
                    Log.e(TAG, "MediaStyle: ", exMediaStyle);
                }
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

                //Toast.makeText(context, "getchild"+holder.notificationRemoteView.getChildCount(), Toast.LENGTH_SHORT).show();

                for(int index = 0; index < ((ViewGroup) holder.notificationRemoteView).getChildCount(); index++) {
                    View nextChild = ((ViewGroup) holder.notificationRemoteView).getChildAt(index);
                    holder.notificationRemoteView.removeView(nextChild);
                }

                try{
                    if(notify.get_notification_view()!=null) {
                        if (notify.get_notification_view().getParent() != null) {
                            ((ViewGroup) notify.get_notification_view().getParent()).removeView(notify.get_notification_view()); // <- fix
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

        //Notification informations
        holder.notificationbitmapView.setImageBitmap(notify.get_notification_picture());
        holder.notificationbitmapView.setColorFilter(Color.parseColor("#000000"), PorterDuff.Mode.SRC_ATOP);
        holder.notificationLargeIconView.setImageBitmap(notify.get_notification_bitmaplargeicon());
        holder.notificationTimestampView.setText(notify.get_notification_time() );
        holder.notificationTextView.setText(notify.get_notification_htmltext() );
    }

    //Returns the total number of items in the data set held by the adapter.
    @Override
    public int getItemCount() {
        return this.notificationlist.size();
    }

    // Find Image ID corresponding to the name of the image (in the directory drawable).
    public int getDrawableResIdByName(String resName)  {
        String pkgName = context.getPackageName();
        // Return 0 if not found.
        return context.getResources().getIdentifier(resName , "drawable", pkgName);
    }

    // Interface definition for a callback to be invoked when a view is clicked.
    private void handleRecyclerItemClick(RecyclerView recyclerView, View itemView) throws PendingIntent.CanceledException, IntentSender.SendIntentException {
        int itemPosition = recyclerView.getChildLayoutPosition(itemView);
        Notification notify  = this.notificationlist.get(itemPosition);
        // Toast.makeText(this.context, "" + notify.get_notification_contentintent(), Toast.LENGTH_LONG).show();
        try {
            if (notify.get_notification_contentintent() != null) {
                notify.get_notification_contentintent().send();
            }
        } catch (PendingIntent.CanceledException e) {
            throw new IntentSender.SendIntentException(e);
        }
    }

    public void setChecked(ArrayList<checked> selected) {
        this.selected = new ArrayList<>();
        this.selected = selected;
    }

    public ArrayList<checked> get_selected(){return this.selected;}

    public void set_selected(ArrayList<checked> selected){ this.selected = selected;}

}