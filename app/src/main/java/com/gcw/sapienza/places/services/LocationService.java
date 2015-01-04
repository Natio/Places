package com.gcw.sapienza.places.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.*;
import android.util.Log;

import com.gcw.sapienza.places.MMapFragment;
import com.gcw.sapienza.places.MainActivity;
import com.gcw.sapienza.places.MosaicFragment;
import com.gcw.sapienza.places.Notifications;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.utils.Utils;
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

    private static final String TAG = "LocationService";

    private final IBinder mBinder = new LocalBinder();

    private static final long INTERVAL = 1000 * 30;
    private static final long FASTEST_INTERVAL = 1000 * 5;
    private static final long ONE_MIN = 1000 * 60;
    private static final long REFRESH_TIME = ONE_MIN * 1; //TODO high frequency, useful for debugging purposes
    private static final int MAX_PINS = 10;

    private static final int NOTIFICATION_ID = 12345;

    private LocationRequest locationRequest;
    private GoogleApiClient googleApiClient;
    private Location location;
    private FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;

    private ILocationUpdater listener;

    private List<Flag> parseObjects;

    private Location notificationLocation;

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "Connected to Google Api");
        Location currentLocation = fusedLocationProviderApi.getLastLocation(googleApiClient);
        if (currentLocation != null) {
            this.location = currentLocation;
            queryParsewithLocation(currentLocation);
            updateApplication();
        }
        fusedLocationProviderApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    public Location getLocation(){
        return this.location;
    }

    public void setListener(ILocationUpdater app) {
        this.listener = app;
    }

    public class LocalBinder extends Binder {
        public LocationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocationService.this;
        }
    }

    public void queryParsewithLocation(Location location)
    {
        ParseGeoPoint gp = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        ParseQuery<Flag> query = ParseQuery.getQuery("Posts");
        query.whereWithinKilometers("location", gp, Utils.MAP_RADIUS);
        if(!Utils.LONE_WOLF_ENABLED) query.whereNotEqualTo("fbId", Utils.fbId);
        query.setLimit(MAX_PINS); //TODO want this to be user-specific?
        query.findInBackground(new FindCallback<Flag>() {
            @Override
            public void done(List<Flag> parseObjects, ParseException e) {
                LocationService.this.parseObjects = parseObjects;
                Log.d(TAG, "Found " + parseObjects.size() +
                        " pins within " + Utils.MAP_RADIUS + " km");
                updateApplication();

                MosaicFragment.configureListViewWithFlags();
                MMapFragment.updateMarkersOnMap();
            }
        });
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient connection has failed");
    }

    @Override
    public void onLocationChanged(Location location) {
        if(notificationLocation != null && (location.distanceTo(this.notificationLocation) / 1000) > Utils.MAP_RADIUS){
            NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nManager.cancel(NOTIFICATION_ID);
        }
        Log.d(TAG, "Location changed");
        long elapsed_time = location.getTime() -
                (this.location == null ? 0l : this.location.getTime());
        Log.d(TAG, "Elapsed time: " + elapsed_time);
        float distance = location.distanceTo(this.location) / 1000;
        Log.d(TAG, "Distance from last known location: " + distance);
        if (elapsed_time > REFRESH_TIME && distance > Utils.MAP_RADIUS / 2) { //TODO comment second condition for debugging ease
            this.location = location;
            queryParsewithLocation(location);
            if(this.parseObjects.size() > 0 && !MainActivity.isForeground()) {
                Log.d(TAG, "Notifying user..." +
                        this.parseObjects.size() + " pins found");
                notifyUser();
                this.notificationLocation = this.location;
            }
            updateApplication();
        }
    }

    private void notifyUser() {
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setAutoCancel(true)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(Notifications.notifications[(int) (Math.random() * Notifications.notifications.length)])
                        .setContentText(this.parseObjects.size() + " time capsules around!")
                        .setSound(soundUri)
                        .setLights(0xff00ff00, 1000, 3000);

        Intent targetIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);
        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.notify(NOTIFICATION_ID, builder.build());
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
            Log.d(TAG, "Google Api Client built");
            googleApiClient.connect();
        }
    }

    @Override
    public void onDestroy(){
        Log.d(TAG, "Being Destroyed");
        super.onDestroy();
        googleApiClient.disconnect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}