package com.gcw.sapienza.places;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.app.Activity;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import com.parse.*;

import java.util.List;

public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final IBinder mBinder = new LocalBinder();

    private static final long INTERVAL = 1000 * 30;
    private static final long FASTEST_INTERVAL = 1000 * 5;
    private static final long ONE_MIN = 1000 * 60;
    private static final long REFRESH_TIME = ONE_MIN * 5;
    private static final float MINIMUM_ACCURACY = 50.0f;
    private static final float SMALLEST_DISPLACEMENT = 100.0f;

    private LocationRequest locationRequest;
    private GoogleApiClient googleApiClient;
    private Location location;
    private FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;

    private ILocationUpdater listener;

    private List<ParseObject> parseObjects;

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("Location Service", "Connecting to Google Api");
        Location currentLocation = fusedLocationProviderApi.getLastLocation(googleApiClient);
        if (currentLocation != null) {
            this.location = currentLocation;
            queryParsewithLocation(currentLocation);
            updateApplication();
        } else {
            fusedLocationProviderApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    public Location getLocation(){
        return this.location;
    }

    public void setListener(ILocationUpdater app) {
        this.listener = app;
    }

    public class LocalBinder extends Binder {
        LocationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocationService.this;
        }
    }

    public void queryParsewithLocation(Location location){
        ParseGeoPoint gp = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Posts");
        query.whereWithinKilometers("location", gp, 0.5f);
        query.setLimit(10);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                LocationService.this.parseObjects = parseObjects;
                Log.d("Location Service", "Found " + parseObjects.size() + " pins nearby");
                Log.d("Location Service", "=====PINS=====");
                for(int i = 0; i < parseObjects.size(); i++){
                    Log.d("Location Service", (String) parseObjects.get(i).get("text"));
                }
            }
        });
        Log.d("Location Service", "Connected to Google Api");
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.i("Location Service", "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i("Location Service", "GoogleApiClient connection has failed");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("Location Service", "Location changed");
        long elapsed_time = location.getTime() -
                (this.location == null ? 0l : this.location.getTime());
        if (elapsed_time > REFRESH_TIME) {
            this.location = location;
            queryParsewithLocation(location);
            updateApplication();
        }
    }

    private void updateApplication(){
        if(listener != null) {
            listener.setLocation(location);
            listener.setPinsNearby(parseObjects);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (googleApiClient != null) {
            Log.d("Location Service", "Google Api Client built");
            googleApiClient.connect();
        }
    }

    @Override
    public void onDestroy(){
        Log.d("Location Service", "Being Destroyed");
        super.onDestroy();
        googleApiClient.disconnect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}