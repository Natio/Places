package com.gcw.sapienza.places;

import android.app.Application;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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

    //Shared location manager
    //fixme find a better way to handle gps
    private LocationManager locationManager;

    //current location
    private Location currentLocation = null;

    //method called when the app is launched
    @Override
    public void onCreate() {
        super.onCreate();

        //initialize the location manager
        this.initLocationManager();

        // initialize Parse.com
        ParseObject.registerSubclass(Flag.class);
        Parse.initialize(this, PARSE_COM_APP_KEY , PARSE_COM_CLIENT_KEY);

        Log.d(TAG, "Hi");

    }

    private void initLocationManager(){
        this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

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
}
