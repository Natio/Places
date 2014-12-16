package com.gcw.sapienza.places;

import android.app.Application;
import android.util.Log;

import com.cgw.sapienza.places.model.Flag;
import com.parse.Parse;
import com.parse.ParseObject;


public class PlacesApplication extends Application{
    //just the tag for logging
    private static final String TAG = "PlacesApplication";

    //Parse.com app key
    private static final String PARSE_COM_APP_KEY = "BWtqN9x6uyr935MKAROcWkc6mzv8KLQMMVnFGHps";

    //Parse.com client key
    private static final String PARSE_COM_CLIENT_KEY = "Gr1g8Z2kfv3AOZqToZ30hyMyNzH24vj4yudNoKfb";

    //method called when the app is launched
    @Override
    public void onCreate() {
        super.onCreate();

        // initialize Parse.com
        ParseObject.registerSubclass(Flag.class);
        Parse.initialize(this, PARSE_COM_APP_KEY , PARSE_COM_CLIENT_KEY);
    }
}
