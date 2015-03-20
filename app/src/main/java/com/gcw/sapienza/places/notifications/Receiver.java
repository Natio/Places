package com.gcw.sapienza.places.notifications;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.gcw.sapienza.places.activities.MainActivity;
import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by snowblack on 1/4/15.
 */
public class Receiver extends ParsePushBroadcastReceiver {
    private static final String TAG = "Receiver";
    public static final String RECEIVED_NOTIF_COMMENT_TYPE = "comment_notification";
    public static final String FLAG_ID = "flagId";
    @Override
    public void onPushOpen(Context context, Intent intent) {
        Log.d("Push", "Clicked");
        Log.d(TAG, intent.getExtras().getString("com.parse.Data"));

        try{
            JSONObject root = new JSONObject(intent.getExtras().getString("com.parse.Data"));
            String type = root.getString("type");
            String flag_id = root.getString("commented_flag");
            if(type != null && type.equals("comment") && flag_id != null){

                Intent i = new Intent(context, MainActivity.class);
                Bundle extras = new Bundle();
                extras.putString("type",RECEIVED_NOTIF_COMMENT_TYPE);
                extras.putString(FLAG_ID, flag_id);

                i.putExtras(intent.getExtras());
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
                //TODO the intent launches MainActivity but the flag is not opened

            }
            else{
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://drive.google.com/folderview?id=0B1boWbY-47RQdHJnSlpScUNueTQ&usp=drive_web"));
                browserIntent.putExtras(intent.getExtras());
                browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(browserIntent);
            }


        }
        catch (JSONException e){
            Log.d(TAG, "Json error", e);
        }


//        Default behavior: simply open up the Main Activity when clicking on the push notification
//        Intent i = new Intent(context, MainActivity.class);
//        i.putExtras(intent.getExtras());
//        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(i);


    }
}
