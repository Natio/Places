package com.gcw.sapienza.places.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.gcw.sapienza.places.activities.MainActivity2;
import com.gcw.sapienza.places.MainActivity;
import com.gcw.sapienza.places.Notifications;
import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.gcw.sapienza.places.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;

import java.security.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = "LocationService";

    public static final String FOUND_NEW_FLAGS_NOTIFICATION = "FOUND_NEW_FLAGS_NOTIFICATION";

    private final IBinder mBinder = new LocalBinder();

    private static final long ONE_MIN = 1000 * 60;

    private static final long ONE_HOUR = ONE_MIN * 60;

    private static final int KM_TO_M = 1000;

    private static final long INTERVAL = ONE_MIN * 5;
    private static final long FASTEST_INTERVAL = ONE_MIN * 3;

    private static final int NOTIFICATION_ID = 12345;

    private static final long UPDATE_CACHE_MIN_INTERVAL = ONE_MIN * 15;

    private static final long FLAG_IN_CACHE_MIN = ONE_HOUR * 2;


    private HashMap<String, Long> cachedFlags = new HashMap<>();

    private static LocationRequest locationRequest;
    private static GoogleApiClient googleApiClient;
    private Location location;
    private static FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;

    private ILocationUpdater listener;

    private List<Flag> parseObjects;

    private Location notificationLocation;

    private long lastCacheUpdate;


    @Override
    public void onConnected(Bundle connectionHint)
    {
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
        //creates a fake location for testing if it is running on simulator
        if(PlacesApplication.isRunningOnEmulator){
            location = PlacesApplication.getInstance().getLocation();
        }

        //this is for avoiding a crash if location is null
        //the crash happens if there is no GPS data and the action range is changed
        if(location == null){
            return;
        }


        ParseGeoPoint gp = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        ParseQuery<Flag> query = ParseQuery.getQuery("Posts");

        float radius = Utils.MAP_RADIUS;

        if(PlacesApplication.isRunningOnEmulator){
            radius = 10.0f;
        }

        query.whereWithinKilometers("location", gp, radius);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        boolean lone_wolf = preferences.getBoolean("meFilter", true);
        boolean with_friends_surrounded = preferences.getBoolean("flFilter", true);
        boolean storytellers_in_the_dark = preferences.getBoolean("strangersFilter", true);
        boolean archaeologist = preferences.getBoolean("timeFilter", false);

        boolean thoughts_check = preferences.getBoolean("thoughtsCheck", true);
        boolean fun_check = preferences.getBoolean("funCheck", true);
        boolean landscape_check = preferences.getBoolean("landscapeCheck", true);
        boolean food_check = preferences.getBoolean("foodCheck", true);
        boolean none_check = preferences.getBoolean("noneCheck", true);

        Log.v(TAG, "Lone Wolf enabled: " + lone_wolf);
        Log.v(TAG, "With Friends Surrounded enabled: " + with_friends_surrounded);
        Log.v(TAG, "Storytellers In The Dark enabled: " + storytellers_in_the_dark);
        Log.v(TAG, "Archaeologist enabled: " + archaeologist);

        Log.v(TAG, "Thoughts enabled: " + thoughts_check);
        Log.v(TAG, "Fun enabled: " + fun_check);
        Log.v(TAG, "Landscape: " + landscape_check);
        Log.v(TAG, "Food: " + food_check);
        Log.v(TAG, "None: " + none_check);

        if(FacebookUtils.getInstance().hasCurrentUserId() == false)
        {
            final android.os.Handler handler = new android.os.Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (FacebookUtils.getInstance().hasCurrentUserId() == false){
                        handler.postDelayed(this, Utils.UPDATE_DELAY);
                    }
                    else{
                        queryParsewithLocation(getLocation());
                    }
                }
            });
            return;
        }

        if(!thoughts_check && !fun_check && !landscape_check
                && ! food_check && ! none_check){
            Toast.makeText(getApplicationContext(), "You won't be able to see any flags with these settings", Toast.LENGTH_LONG).show();

            if(parseObjects == null){
                parseObjects = new ArrayList<>(0);
            }
            else{
                parseObjects.clear();
            }

            updateApplication();

            return;

        }

        ArrayList<String> selectedCategories = new ArrayList<>();
        if(thoughts_check) selectedCategories.add("Thoughts");
        if(fun_check) selectedCategories.add("Fun");
        if(landscape_check) selectedCategories.add("Landscape");
        if(food_check) selectedCategories.add("Food");
        if(none_check) selectedCategories.add("None");
        query.whereContainedIn("category", selectedCategories);

        if(!storytellers_in_the_dark)
        {
            if(lone_wolf && with_friends_surrounded)
            {
                ArrayList<String> meAndMyFriends = new ArrayList<>();
                meAndMyFriends.add(FacebookUtils.getInstance().getCurrentUserId());
                meAndMyFriends.addAll(FacebookUtils.getInstance().getFriends());
                query.whereContainedIn("fbId", meAndMyFriends);
            }

            else if(lone_wolf) query.whereEqualTo("fbId", FacebookUtils.getInstance().getCurrentUserId());
            else if(with_friends_surrounded) query.whereContainedIn("fbId", FacebookUtils.getInstance().getFriends());
            else
            {
                Toast.makeText(getApplicationContext(), "No filterYou won't be able to see any flags with these settings", Toast.LENGTH_LONG).show();

                if(parseObjects == null){
                    parseObjects = new ArrayList<>(0);
                }
                else{
                    parseObjects.clear();
                }

                updateApplication();

                return;
            }
        }
        else
        {
            if (!lone_wolf)
                query.whereNotEqualTo("fbId", FacebookUtils.getInstance().getCurrentUserId());
            if (!with_friends_surrounded)
                query.whereNotContainedIn("fbId", FacebookUtils.getInstance().getFriends()); // this is rather expensive
        }

        if(archaeologist) query.orderByAscending("createdAt");
        else query.orderByDescending("createdAt");

        query.setLimit(Utils.MAX_PINS);
        if(PlacesApplication.isRunningOnEmulator){
            query.setLimit(50);
        }


        query.findInBackground(new FindCallback<Flag>() {
            @Override
            public void done(List<Flag> parseObjects, ParseException e) {
                if(parseObjects == null){
                    parseObjects = new ArrayList<>();
                }
                LocationService.this.parseObjects = parseObjects;
                Log.d(TAG, "Found " + parseObjects.size() +
                        " pins within " + Utils.MAP_RADIUS + " km");
                updateApplication();


                LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.FOUND_NEW_FLAGS_NOTIFICATION));

            }
        });
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient connection has failed "+ connectionResult.toString());
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Interval: " + INTERVAL);
        Log.d(TAG, "Fastest Interval: " + FASTEST_INTERVAL);
        Log.d(TAG, "Location accuracy: " + location.getAccuracy());
        if(notificationLocation != null && location.distanceTo(this.notificationLocation) > (Utils.MAP_RADIUS * KM_TO_M)       ){
            NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nManager.cancel(NOTIFICATION_ID);
        }
        Log.d(TAG, "Location changed");
        long elapsed_time = location.getTime() -
                (this.location == null ? 0L : this.location.getTime());
        Log.d(TAG, "Elapsed time: " + elapsed_time);
        if(this.location != null) {
            float distance = location.distanceTo(this.location) / KM_TO_M;
            Log.d(TAG, "Distance from last known location: " + distance);
        }
        this.location = location;
        queryParsewithLocation(location);
        if(this.parseObjects != null && this.parseObjects.size() > 0
                && !MainActivity2.isForeground() && FacebookUtils.getInstance().hasCurrentUserId()) {
            Log.d(TAG, "Notifying user..." +
                    this.parseObjects.size() + " pins found");
            notifyUser();
            this.notificationLocation = this.location;
        }
        updateApplication();
    }

    private void notifyUser() {
        int newFlags = updateCachedPostsAndRet();
        if(newFlags > 0) {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            int numFlags = this.parseObjects.size();
            String textIfOne = "A flag for you!";
            String textMoreThanOne = "Hey, " + this.parseObjects.size() + " new Flags around!";

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this)
                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.app_logo_small)
                            .setContentTitle(Notifications.notifications[(int) (Math.random() * Notifications.notifications.length)])
                            .setContentText(numFlags > 1 ? textMoreThanOne : textIfOne)
                            .setSound(soundUri)
                            .setLights(0xff00ff00, 1000, 3000);

            Intent targetIntent = new Intent(this, MainActivity2.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(contentIntent);
            NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nManager.notify(NOTIFICATION_ID, builder.build());
        }else{
            Log.w(TAG, "Notification to user aborted: no new Flags found: " + newFlags);
        }
    }

    private int updateCachedPostsAndRet() {
        long newTimestamp = new java.util.Date().getTime();

        if(newTimestamp - this.lastCacheUpdate > UPDATE_CACHE_MIN_INTERVAL){
            removeCachedPosts();
        }

        int nonCachedFlags = parseObjects.size();

        Set<String> cachedKeys = this.cachedFlags.keySet();

        for(Flag f: parseObjects){
            if(cachedKeys.contains(f.getObjectId())){
                //Flag already cached.
                nonCachedFlags--;
            }else{
                //Not cached yet. Being cached now.
                this.cachedFlags.put(f.getObjectId(), newTimestamp);
            }
        }
        
        return nonCachedFlags;
    }

    private void removeCachedPosts() {
        this.lastCacheUpdate = new java.util.Date().getTime();
        for(String tf: this.cachedFlags.keySet()){
            if(this.lastCacheUpdate - cachedFlags.get(tf) > FLAG_IN_CACHE_MIN){
                this.cachedFlags.remove(tf);
            }
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
        connectToGoogleAPI();
    }

    private void connectToGoogleAPI() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        Log.d(TAG, "Smallest displacement: " + Utils.MAP_RADIUS * KM_TO_M / 2);
        locationRequest.setSmallestDisplacement(Utils.MAP_RADIUS * KM_TO_M / 2);

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

    //courtesy of http://gis.stackexchange.com/questions/25877/how-to-generate-random-locations-nearby-my-location
    public static Location getRandomLocation(Location center, int radius) {
        double x0 = center.getLongitude();
        double y0 = center.getLatitude();
        Random random = new Random();

        // Convert radius from meters to degrees
        double radiusInDegrees = radius / 111000f;

        double u = random.nextDouble();
        double v = random.nextDouble();
        double w = radiusInDegrees * Math.sqrt(u);
        double t = 2 * Math.PI * v;
        double x = w * Math.cos(t);
        double y = w * Math.sin(t);

        // Adjust the x-coordinate for the shrinking of the east-west distances
        double new_x = x / Math.cos(y0);

        double foundLongitude = new_x + x0;
        double foundLatitude = y + y0;

        Location random_loc = new Location("fake_location_in_rome");
        random_loc.setLatitude(foundLatitude);
        random_loc.setLongitude(foundLongitude);
        return random_loc;
    }
}