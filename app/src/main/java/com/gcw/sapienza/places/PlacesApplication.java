package com.gcw.sapienza.places;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.gcw.sapienza.places.remotesettings.RemoteSettings;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.remotesettings.RemoteSettingsCallBacks;
import com.gcw.sapienza.places.services.ILocationUpdater;
import com.gcw.sapienza.places.services.LocationService;
import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.ParseException;
import com.gcw.sapienza.places.services.LocationService.LocalBinder;
//Parse push notifications
import com.parse.ParsePush;
import com.parse.SaveCallback;

import java.util.List;


public class PlacesApplication extends Application{
    //just the tag for logging
    private static final String TAG = "PlacesApplication";

    //the app context
    private static Context PLACES_CONTEXT = null;

    //Parse.com app key
    private static final String PARSE_COM_APP_KEY = "BWtqN9x6uyr935MKAROcWkc6mzv8KLQMMVnFGHps";

    //Parse.com client key
    private static final String PARSE_COM_CLIENT_KEY = "Gr1g8Z2kfv3AOZqToZ30hyMyNzH24vj4yudNoKfb";

    //Shared location manager
    //fixme find a better way to handle GPS
    private LocationManager locationManager;

    //current location
    private Location currentLocation = null;
    private List<ParseObject> pinsNearby;


    public LocationService mService;
    boolean mBound = false;


    public static Context getPlacesAppContext(){
        return PlacesApplication.PLACES_CONTEXT;
    }

    //method called when the app is launched
    @Override
    public void onCreate() {
        super.onCreate();
        PlacesApplication.PLACES_CONTEXT = this.getApplicationContext();
        //initialize the location manager
//        this.initLocationManager();
        //fixme location service not connecting to google api properly
        startLocationService();
        // initialize Parse.com
        ParseObject.registerSubclass(Flag.class);
        Parse.initialize(this, PARSE_COM_APP_KEY , PARSE_COM_CLIENT_KEY);
        //Parse push notifications
        subscribeToParseBroadcast();
        //Syncs settings with the server
        RemoteSettings.getInstance().synchWithFileAtURL("https://dl.dropboxusercontent.com/u/2181964/remote_config.json", new RemoteSettingsCallBacks() {
            @Override
            public void onRemoteConfig() {
                Log.d(TAG, "RemoteSettings configured");
            }

            @Override
            public void onError(String error) {
                Log.d(TAG, "RemoteSettings configuration error: "+error);
            }
        });
    }

    private void subscribeToParseBroadcast() {
        ParsePush.subscribeInBackground("", new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d("com.parse.push", "successfully subscribed to the broadcast channel.");
                } else {
                    Log.e("com.parse.push", "failed to subscribe for push", e);
                }
            }
        });
    }

    private void startLocationService() {
        Intent locInt = new Intent(this, LocationService.class);
        Log.d("Places Application", "Starting Location Service");
//        stopService(locInt);
        startService(locInt);
        bindService(locInt, mConnection, BIND_AUTO_CREATE);
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mService.setListener(listener);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            mBound = false;
        }
    };

    private void initLocationManager(){
        this.locationManager = (LocationManager) this.getSystemService(PlacesApplication.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                PlacesApplication.this.currentLocation = location;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        this.locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

    }

    public Location getCurrentLocation(){
        return this.currentLocation;
    }
    private ILocationUpdater listener = new ILocationUpdater() {
        public void setLocation(Location l){
            PlacesApplication.this.currentLocation = l;
        }
        public void setPinsNearby(List<ParseObject> l){
            PlacesApplication.this.pinsNearby = l;
        }
    };
}
