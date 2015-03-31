package com.gcw.sapienza.places.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.utils.PlacesStorage;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by snowblack on 1/4/15.
 */
public class Receiver extends ParsePushBroadcastReceiver {

    private static final String TAG = "Receiver";
    private static final String COLLAPSE_KEY = "places_push";
    private static final int COMMENT_NOTIFICATION_ID = 80;
    private static final String COMMENT_TYPE = "comment";
    private String notificationText = "";
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
                extras.putString("type", Utils.RECEIVED_NOTIF_COMMENT_TYPE);
                extras.putString(Utils.FLAG_ID, flag_id);

//                Notification currNotification = getNotification(context, intent);

//                PlacesStorage.updateInboxWith(context, flag_id, alert_text);

                i.putExtras(extras);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

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

                clearNotificationText();

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
            Utils.showToast(context, "Something went wrong while loading Places data", Toast.LENGTH_SHORT);
        }

//        Default behavior: simply open up the Main Activity when clicking on the push notification
//        Intent i = new Intent(context, MainActivity.class);
//        i.putExtras(intent.getExtras());
//        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(i);


    }

    private void clearNotificationText() {
        notificationText = "";
    }

    @Override
    protected void onPushReceive(Context context, Intent intent) {

        try {

            JSONObject data = new JSONObject(intent.getExtras().getString("com.parse.Data"));

            String type = data.getString("type");

            switch (type) {
                case COMMENT_TYPE:
                    String flag_id = data.getString("commented_flag");
                    String alert_text = data.getString("alert");
                    String alert_title = data.getString("title");

                    notificationText = notificationText + "\n" + alert_text;

                    //TODO uncomment when Inbox ready
//                    PlacesStorage.updateInboxWith(context, flag_id, alert_text);

                    NotificationManager notificationManager =
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                    Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                    builder.setContentTitle(alert_title);
                    builder.setContentText(notificationText);
                    builder.setSmallIcon(R.drawable.app_logo_small);
                    builder.setAutoCancel(true);

                    // OPTIONAL create soundUri and set sound:
                    builder.setSound(soundUri);

                    notificationManager.notify(COMMENT_NOTIFICATION_ID, builder.build());
                    break;
                default:
                    Log.d(TAG, "Triggering default onPushReceive behavior");
            }
        }catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private JSONObject getDataFromIntent(Context context, Intent intent) {
        JSONObject data = null;
        try {
            data = new JSONObject(intent.getExtras().getString("com.parse.Data"));
        } catch (JSONException e) {
            Log.d(TAG, "Json error", e);
            Utils.showToast(context, "Something went wrong while loading Places data", Toast.LENGTH_SHORT);
            return null;
        }
        return data;
    }

    @Override
    protected void onPushDismiss(Context context, Intent intent){
        super.onPushDismiss(context, intent);

//        clear notification text? maybe not advised
//        clearNotificationText();
    }
}
