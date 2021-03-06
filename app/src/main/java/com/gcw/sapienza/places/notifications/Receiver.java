/*
 * Copyright 2015-present Places®.
 */
package com.gcw.sapienza.places.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.utils.PlacesStorage;
import com.gcw.sapienza.places.utils.PlacesUtils;
import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/*
 * Notification management, from Parse cloud service
 */

public class Receiver extends ParsePushBroadcastReceiver {

    private static final String TAG = "Receiver";
    private static final String COLLAPSE_KEY = "places_push";
    private static final int COMMENT_NOTIFICATION_ID = 80;
    private static final int PUSH_OPEN_REQUEST = 90;
    private static final String COMMENT_TYPE = "comment";
    private static final String UPDATES_REPO = "https://drive.google.com/folderview?id=0B1boWbY-47RQdHJnSlpScUNueTQ&usp=drive_web";

    @Override
    public void onPushOpen(Context context, Intent intent) {
        Log.d("Push", "Clicked");
        Log.d(TAG, intent.getExtras().getString("com.parse.Data"));

        try {
            JSONObject root = new JSONObject(intent.getExtras().getString("com.parse.Data"));
            String type = root.getString("type");
            String flag_id = root.getString("commented_flag");
            String alert_text = root.getString("alert");
            if (type != null && type.equals("comment") && flag_id != null) {

                Intent i = new Intent(context, MainActivity.class);
                Bundle extras = new Bundle();

                /*
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, i, 0);
                try
                {
                    contentIntent.send();
                }
                catch(PendingIntent.CanceledException ce)
                {
                    ce.printStackTrace();
                }
                */

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

                int notificationsSize = prefs.getStringSet("pushTextSet", new HashSet<String>()).size();

                SharedPreferences.Editor editor = prefs.edit();
                editor.putStringSet("pushTextSet", new HashSet<String>());
                editor.commit();
                if (notificationsSize == 1) {
                    Log.d(TAG, "Just 1 notification. Showing the related Flag");
                    extras.putString("type", PlacesUtils.RECEIVED_NOTIF_COMMENT_TYPE);
                    extras.putString(PlacesUtils.FLAG_ID, flag_id);
                } else if (notificationsSize > 1) {
                    Log.d(TAG, notificationsSize + " notifications. Showing the Inbox");
                    extras.putString("type", PlacesUtils.RECEIVED_MULTI_NOTIF_COMMENT_TYPE);
                } else {
                    Log.w(TAG, "0 notifications. Nothing to show");
                    return;
                }

                i.putExtras(extras);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(i);
            } else {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(UPDATES_REPO));
                browserIntent.putExtras(intent.getExtras());
                browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(browserIntent);
            }
        } catch (JSONException e) {
            Log.d(TAG, "Json error", e);
            PlacesUtils.showToast(context, "Something went wrong while loading Places data", Toast.LENGTH_SHORT);
        }

//        Default behavior: simply open up the Main Activity when clicking on the push notification
//        Intent i = new Intent(context, MainActivity.class);
//        i.putExtras(intent.getExtras());
//        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(i);


    }

    @Override
    protected void onPushReceive(Context context, Intent intent) {
        Log.d(TAG, "onPushReceive called");
        try {
            JSONObject data = new JSONObject(intent.getExtras().getString("com.parse.Data"));

            String type = data.getString("type");

            switch (type) {
                case COMMENT_TYPE:

                    String flag_id = data.getString("commented_flag");
                    String alert_text = data.getString("alert");
                    String commenter_id = data.getString("commenter");
                    String commenter_name = data.getString("commenter_name");
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    Set<String> alerts = prefs.getStringSet("pushTextSet", new HashSet<String>());

                    alerts.add(commenter_name + ": " + alert_text);

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putStringSet("pushTextSet", alerts);
                    editor.commit();

//                    TODO comment if problem arise with inbox
                    PlacesStorage.updateInboxWith(context, commenter_id, flag_id, alert_text);

                    Notification commentsNotification = getNotification(context, intent);
                    NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    nManager.notify(COMMENT_NOTIFICATION_ID, commentsNotification);

                    break;
                default:
                    Log.d(TAG, "Triggering default onPushReceive behavior...");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive Called");
        super.onReceive(context, intent);
    }

    @Override
    protected Notification getNotification(Context context, Intent intent) {
        Log.d(TAG, "getNotification called");

        try {

            JSONObject data = new JSONObject(intent.getExtras().getString("com.parse.Data"));

            String type = data.getString("type");

            switch (type) {
                case COMMENT_TYPE:

                    String alert_title = data.getString("title");

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    Set<String> alerts = prefs.getStringSet("pushTextSet", new HashSet<String>());

                    NotificationManager notificationManager =
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                    Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                    builder.setContentTitle(alerts.size() + ((alerts.size() > 1) ? " new notifications!" : " new notification!"));
                    builder.setContentText(alert_title);
                    builder.setSmallIcon(R.drawable.app_ico_small);
                    builder.setAutoCancel(true);

                    NotificationCompat.InboxStyle inboxStyle =
                            new NotificationCompat.InboxStyle();
                    inboxStyle.setBigContentTitle("Details: ");
                    for (String s : alerts) {
                        inboxStyle.addLine(s);
                    }
                    builder.setStyle(inboxStyle);

                    Intent i = new Intent();
                    i.setAction("com.parse.push.intent.OPEN");
                    i.putExtras(intent.getExtras());
                    PendingIntent pendingIntentNo = PendingIntent.getBroadcast(context, PUSH_OPEN_REQUEST, i, PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.setContentIntent(pendingIntentNo);

                    // OPTIONAL create soundUri and set sound:
                    builder.setSound(soundUri);

                    return builder.build();
                default:
                    Log.d(TAG, "Triggering default onPushReceive behavior");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPushDismiss(Context context, Intent intent) {
        super.onPushDismiss(context, intent);

//        clear notification text? maybe not advised
//        clearNotificationText();
    }
}
