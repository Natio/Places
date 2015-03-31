package com.gcw.sapienza.places.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.utils.PlacesStorage;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by snowblack on 1/4/15.
 */
public class Receiver extends ParsePushBroadcastReceiver {

    private static final String TAG = "Receiver";
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

                // PlacesStorage.updateInboxWith(context, flag_id, alert_text);

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
        } /*catch (ClassNotFoundException e) {
            Log.d(TAG, "Class not found", e);
            Utils.showToast(context, "Something went wrong while loading Places data", Toast.LENGTH_SHORT);
        } catch (OptionalDataException e) {
            Log.d(TAG, "Optional data exception", e);
            Utils.showToast(context, "Something went wrong while loading Places data", Toast.LENGTH_SHORT);
        } catch (StreamCorruptedException e) {
            Log.d(TAG, "Stream corrupted", e);
            Utils.showToast(context, "Something went wrong while loading Places data", Toast.LENGTH_SHORT);
        } catch (IOException e) {
            Log.d(TAG, "I/O error", e);
            Utils.showToast(context, "Something went wrong while loading Places data", Toast.LENGTH_SHORT);
        }*/


//        Default behavior: simply open up the Main Activity when clicking on the push notification
//        Intent i = new Intent(context, MainActivity.class);
//        i.putExtras(intent.getExtras());
//        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(i);


    }
}
