package com.gcw.sapienza.places.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by snowblack on 3/31/15.
 */
public class PlacesStorage {
    public static final String INBOX_FILE = "inbox";
    private static final String TAG = "PlacesStorage";

    public static List<List<String>> fetchInbox(Context context)
            throws IOException, ClassNotFoundException{
        File storageFile = new File(context.getFilesDir(), PlacesStorage.INBOX_FILE);

        if(!storageFile.exists()){
            Log.w(TAG, "Storage file doesn't exist! Creating new one...");
            storageFile.createNewFile();
        }

        FileInputStream fis = context.openFileInput(PlacesStorage.INBOX_FILE);
        ObjectInputStream is = new ObjectInputStream(fis);

        List<List<String>> inbox = (List<List<String>>) is.readObject();

        is.close();
        fis.close();

        return inbox;
    }

    public static void updateInboxWith(Context context, String flag_id, String alert_text)
            throws ClassNotFoundException, IOException {



        List<List<String>> inbox = fetchInbox(context);

        if(inbox == null){
            inbox = new ArrayList<>();
        }

        List<String> notificationEntry = new ArrayList<>();
        notificationEntry.add(flag_id);
        notificationEntry.add(alert_text);
        inbox.add(notificationEntry);

        FileOutputStream fos = context.openFileOutput(PlacesStorage.INBOX_FILE, Context.MODE_PRIVATE);
        ObjectOutputStream os = new ObjectOutputStream(fos);
        os.writeObject(inbox);
        os.close();
        fos.close();
    }
}
