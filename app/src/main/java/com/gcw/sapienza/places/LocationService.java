package com.gcw.sapienza.places;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.app.Activity;
import android.os.Bundle;
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

public class LocationService extends IntentService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

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

    public LocationService(){
        super("LocationService");
    }

    private final String TAG = "MyAwesomeApp";

    private TextView mLocationView;

    private GoogleApiClient mGoogleApiClient;

    private LocationRequest mLocationRequest;

    static private List<ParseObject> parseObjects;


    @Override
    public void onConnected(Bundle connectionHint) {
        Location currentLocation = fusedLocationProviderApi.getLastLocation(googleApiClient);
        if (currentLocation != null && currentLocation.getTime() > REFRESH_TIME) {
            this.location = currentLocation;
            queryParsewithLocation(currentLocation);
        } else {
            fusedLocationProviderApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    public void queryParsewithLocation(Location location){
        ParseGeoPoint gp = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Posts");
        query.whereNear("location", gp);
        query.setLimit(10);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                LocationService.parseObjects = parseObjects;
                Log.d("FUSED LOCATION PROVIDER", "Found " + parseObjects.size() + " pins nearby");
            }
        });
        Log.d("FUSED LOCATION PROVIDER", "Connected to Google Api");
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
        Log.d("FUSED LOCATION PROVIDER", "Location changed");
        //if the existing location is empty or
        //the current location accuracy is greater than existing accuracy
        //then store the current location
        if (null == this.location || location.getAccuracy() < this.location.getAccuracy()) {
            this.location = location;
            queryParsewithLocation(location);
            //if the accuracy is not better, remove all location updates for this listener
            if (this.location.getAccuracy() < MINIMUM_ACCURACY) {
                fusedLocationProviderApi.removeLocationUpdates(googleApiClient, this);
            }
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
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
            googleApiClient.connect();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        googleApiClient.disconnect();
    }
}