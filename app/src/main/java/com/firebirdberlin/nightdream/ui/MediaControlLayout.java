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

import android.app.ActivityOptions;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.databinding.NotificationMediacontrolBinding;

import java.util.ArrayList;
import java.util.Locale;

public class MediaControlLayout extends ViewModel {
    private static final String TAG = "MediaControlLayout";
    private final MutableLiveData<Drawable> smallIcon = new MediatorLiveData<>();
    private final MutableLiveData<Drawable> largeIcon = new MediatorLiveData<>();
    private final MutableLiveData<Integer> textColor = new MediatorLiveData<>();
    private final MutableLiveData<String> appName = new MediatorLiveData<>();
    private final MutableLiveData<String> timeStamp = new MediatorLiveData<>();
    private final MutableLiveData<String> title = new MediatorLiveData<>();
    private final MutableLiveData<String> text = new MediatorLiveData<>();
    private final MutableLiveData<ArrayList<Drawable>> actionImages = new MediatorLiveData<>();
    private final MutableLiveData<ArrayList<View.OnClickListener>> actionIntent = new MediatorLiveData<>();
    private final NotificationMediacontrolBinding mediaControlBinding;
    private int color = -1543;
    private android.media.session.MediaController activeMediaController = null;
    private android.media.session.MediaController.Callback mediaControllerCallback = null;

    public MediaControlLayout(NightDreamActivity activity, ConstraintLayout mediaStyleContainer) {
        LayoutInflater inflater = LayoutInflater.from(mediaStyleContainer.getContext());

        mediaControlBinding = NotificationMediacontrolBinding.inflate(
                inflater, mediaStyleContainer, false
        );

        mediaControlBinding.setModel(this);
        mediaControlBinding.setLifecycleOwner(activity);
    }

    public View getView() {
        return mediaControlBinding.getRoot();
    }

    public void setColor(int color) {
        this.color = color;
        setColor();
    }

    private void setColor() {
        this.textColor.postValue(this.color);

        if (this.smallIcon.getValue() != null) {
            this.smallIcon.getValue().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }

        if (this.actionImages.getValue() != null) {
            for (int i = 0; i < this.actionImages.getValue().size(); i++) {
                if (this.actionImages.getValue().get(i) != null) {
                    this.actionImages.getValue().get(i).setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                }
            }
        }
    }

    public LiveData<Drawable> getSmallIcon() {
        return smallIcon;
    }

    public LiveData<Drawable> getLargeIcon() {
        return largeIcon;
    }

    public LiveData<Integer> getTextColor() {
        return textColor;
    }

    public LiveData<String> getAppName() {
        return appName;
    }

    public LiveData<String> getTimeStamp() {
        return timeStamp;
    }

    public LiveData<String> getTitle() {
        return title;
    }

    public LiveData<String> getText() {
        return text;
    }

    public LiveData<ArrayList<Drawable>> getActionImages() {
        return actionImages;
    }

    public LiveData<ArrayList<View.OnClickListener>> getActionIntent() {
        return actionIntent;
    }

    private void extractAndPostMetadata(Context context, android.media.MediaMetadata metadata) {
        if (metadata == null || context == null) return;
        Bitmap art = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (art == null) {
            art = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART);
        }
        if (art == null) {
            String uriStr = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI);
            if (Utility.isEmpty(uriStr)) {
                uriStr = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ART_URI);
            }
            if (!Utility.isEmpty(uriStr)) {
                art = Utility.getBitmapFromUri(context, uriStr);
            }
        }
        if (art != null) {
            this.largeIcon.postValue(new BitmapDrawable(context.getResources(), art));
        }
        String trackTitle = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE);
        if (!Utility.isEmpty(trackTitle)) {
            this.title.postValue(trackTitle);
        }
        String trackArtist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST);
        if (!Utility.isEmpty(trackArtist)) {
            this.text.postValue(trackArtist);
        }
    }

    private boolean triggerMediaSessionAction(Notification.Action action) {
        if (activeMediaController == null) {
            return false;
        }
        android.media.session.MediaController.TransportControls controls =
                activeMediaController.getTransportControls();
        if (controls == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= 28) {
            int semanticAction = action.getSemanticAction();
            switch (semanticAction) {
                case Notification.Action.SEMANTIC_ACTION_PLAY:
                    Log.d(TAG, "Triggering MediaController.play()");
                    controls.play();
                    return true;
                case Notification.Action.SEMANTIC_ACTION_PAUSE:
                    Log.d(TAG, "Triggering MediaController.pause()");
                    controls.pause();
                    return true;
                case Notification.Action.SEMANTIC_ACTION_STOP:
                    Log.d(TAG, "Triggering MediaController.stop()");
                    controls.stop();
                    return true;
                default:
                    break;
            }
        }

        CharSequence actionTitle = action.title;
        if (actionTitle != null) {
            String titleStr = actionTitle.toString().toLowerCase(Locale.ROOT);
            if (titleStr.contains("pause") || titleStr.contains("anhalten")) {
                Log.d(TAG, "Triggering MediaController.pause() based on title");
                controls.pause();
                return true;
            } else if (titleStr.contains("play") || titleStr.contains("abspielen")
                    || titleStr.contains("wiedergabe")) {
                Log.d(TAG, "Triggering MediaController.play() based on title");
                controls.play();
                return true;
            } else if (titleStr.contains("zurückspulen") || titleStr.contains("zurueckspulen")
                    || titleStr.contains("rewind") || titleStr.contains("sekunden zurück")
                    || titleStr.contains("sekunden zurueck") || titleStr.contains("s zurück")
                    || titleStr.contains("s zurueck")) {
                Log.d(TAG, "Triggering MediaController.rewind() based on title");
                controls.rewind();
                return true;
            } else if (titleStr.contains("vorspulen") || titleStr.contains("fast forward")
                    || titleStr.contains("forward") || titleStr.contains("sekunden vor")
                    || titleStr.contains("s vor")) {
                Log.d(TAG, "Triggering MediaController.fastForward() based on title");
                controls.fastForward();
                return true;
            } else if (titleStr.contains("next") || titleStr.contains("weiter")
                    || titleStr.contains("nächster") || titleStr.contains("naechster")) {
                Log.d(TAG, "Triggering MediaController.skipToNext() based on title");
                controls.skipToNext();
                return true;
            } else if (titleStr.contains("prev") || titleStr.contains("zurück")
                    || titleStr.contains("zurueck") || titleStr.contains("vorheriger")) {
                Log.d(TAG, "Triggering MediaController.skipToPrevious() based on title");
                controls.skipToPrevious();
                return true;
            }
        }

        return false;
    }

    public void setupFromNotificationIntent(Context context, Intent intent, Drawable smallIcon) {
        Log.d(TAG, "setupFromNotificationIntent");

        String template = intent.getStringExtra("template");
        if (template != null && !template.contains("MediaStyle")) {
            return;
        }

        assert smallIcon != null;
        smallIcon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        Bitmap coverBitmap = intent.getParcelableExtra("largeIconBitmap");

        ArrayList<Drawable> notificationActionImages = new ArrayList<>();
        ArrayList<View.OnClickListener> notificationActionClick = new ArrayList<>();

        Context remotePackageContext = null;
        try {
            remotePackageContext =
                    context.getApplicationContext().createPackageContext(
                            intent.getStringExtra("packageName"), 0
                    );
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        Notification.Action[] actions =
                (Notification.Action[]) intent.getParcelableArrayExtra("actions");
        if (actions != null) {
            int index = 0;
            for (final Notification.Action action : actions) {
                final int actionIndex = index++;
                Drawable notificationDrawableIcon = null;
                Icon icon = action.getIcon();
                if (icon != null) {
                    notificationDrawableIcon = icon.loadDrawable(remotePackageContext);
                    if (notificationDrawableIcon != null) {
                        notificationDrawableIcon.setColorFilter(
                                color, PorterDuff.Mode.SRC_ATOP
                        );
                    }
                }
                notificationActionImages.add(notificationDrawableIcon);

                notificationActionClick.add(
                        v -> {
                            Log.d(
                                    TAG,
                                    "MediaControl action clicked: index=" + actionIndex
                                            + ", title='" + action.title + "'"
                                            + ", intent=" + action.actionIntent
                            );
                            if (triggerMediaSessionAction(action)) {
                                Log.d(
                                        TAG,
                                        "Handled action via MediaController transport controls"
                                );
                                return;
                            }

                            Context sendContext = context.getApplicationContext();
                            if (action.actionIntent != null) {
                                try {
                                    Bundle optionsBundle = null;
                                    if (Build.VERSION.SDK_INT >= 34) {
                                        ActivityOptions options = ActivityOptions.makeBasic();
                                        options.setPendingIntentBackgroundActivityStartMode(
                                                ActivityOptions
                                                        .MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                                        );
                                        optionsBundle = options.toBundle();
                                    }
                                    action.actionIntent.send(
                                            sendContext, 0, null, null, null, null, optionsBundle
                                    );
                                    Log.d(
                                            TAG,
                                            "MediaControl actionIntent.send() success for index="
                                                    + actionIndex
                                    );
                                } catch (PendingIntent.CanceledException ex) {
                                    Log.e(
                                            TAG,
                                            "MediaControl actionIntent canceled for index="
                                                    + actionIndex,
                                            ex
                                    );
                                } catch (Exception ex) {
                                    Log.e(
                                            TAG,
                                            "MediaControl actionIntent failed for index="
                                                    + actionIndex,
                                            ex
                                    );
                                }
                            } else {
                                Log.w(
                                        TAG,
                                        "MediaControl actionIntent is null for index="
                                                + actionIndex
                                );
                            }
                        }
                );
            }
        }

        this.smallIcon.postValue(smallIcon);
        this.appName.postValue(intent.getStringExtra("applicationName"));
        this.timeStamp.postValue(intent.getStringExtra("postTime"));
        this.title.postValue(intent.getStringExtra("title"));
        this.text.postValue(intent.getStringExtra("text"));

        android.media.session.MediaSession.Token token = null;
        if (intent.hasExtra("mediaSessionToken")) {
            token = intent.getParcelableExtra("mediaSessionToken");
        }

        if (token != null) {
            try {
                if (activeMediaController != null && mediaControllerCallback != null) {
                    activeMediaController.unregisterCallback(mediaControllerCallback);
                }
                activeMediaController = new android.media.session.MediaController(context, token);
                mediaControllerCallback = new android.media.session.MediaController.Callback() {
                    @Override
                    public void onMetadataChanged(android.media.MediaMetadata metadata) {
                        extractAndPostMetadata(context, metadata);
                    }
                };
                activeMediaController.registerCallback(mediaControllerCallback);
            } catch (Exception ignored) {
            }
        }

        if (coverBitmap != null) {
            this.largeIcon.postValue(new BitmapDrawable(context.getResources(), coverBitmap));
        } else if (activeMediaController != null && activeMediaController.getMetadata() != null) {
            extractAndPostMetadata(context, activeMediaController.getMetadata());
        } else {
            this.largeIcon.postValue(null);
        }

        this.actionImages.postValue(notificationActionImages);
        this.actionIntent.postValue(notificationActionClick);
        setColor();
        Log.d(TAG, "setupFromNotificationIntent done: " + intent.getStringExtra("title"));
    }
}
