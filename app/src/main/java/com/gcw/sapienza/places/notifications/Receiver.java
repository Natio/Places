package com.gcw.sapienza.places.notifications;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.parse.ParsePushBroadcastReceiver;

/**
 * Created by snowblack on 1/4/15.
 */
public class Receiver extends ParsePushBroadcastReceiver {

    @Override
    public void onPushOpen(Context context, Intent intent) {
        Log.d("Push", "Clicked");

//        Default behavior: simply open up the Main Activity when clicking on the push notification
//        Intent i = new Intent(context, MainActivity.class);
//        i.putExtras(intent.getExtras());
//        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(i);

        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://drive.google.com/folderview?id=0B1boWbY-47RQdHJnSlpScUNueTQ&usp=drive_web"));
        browserIntent.putExtras(intent.getExtras());
        browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(browserIntent);
    }
}
