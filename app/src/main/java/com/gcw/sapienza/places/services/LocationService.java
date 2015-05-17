package com.gcw.sapienza.places.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.fragments.CategoriesFragment;
import com.gcw.sapienza.places.models.Comment;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.models.WoWObject;
import com.gcw.sapienza.places.models.manager.CommentsManager;
import com.gcw.sapienza.places.models.manager.ErrorCallback;
import com.gcw.sapienza.places.models.manager.ModelCallback;
import com.gcw.sapienza.places.models.manager.WoWsManager;
import com.gcw.sapienza.places.notifications.Notifications;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;
import com.gcw.sapienza.places.utils.PlacesStorage;
import com.gcw.sapienza.places.utils.PlacesUtils;
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
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public static final String FOUND_NEW_FLAGS_NOTIFICATION = "FOUND_NEW_FLAGS_NOTIFICATION";
    public static final String FOUND_NO_FLAGS_NOTIFICATION = "FOUND_NO_FLAGS_NOTIFICATION";
    public static final String FOUND_MY_FLAGS_NOTIFICATION = "FOUND_MY_FLAGS_NOTIFICATION";
    public static final String FOUND_NO_MY_FLAGS_NOTIFICATION = "FOUND_NO_MY_FLAGS_NOTIFICATION";
    public static final String LOCATION_CHANGED_NOTIFICATION = "LOCATION_CHANGED_NOTIFICATION";
    public static final String FOUND_BAG_FLAGS_NOTIFICATION = "FOUND_BAG_FLAGS_NOTIFICATION";
    public static final String FOUND_NO_BAG_FLAGS_NOTIFICATION = "FOUND_NO_BAG_FLAGS_NOTIFICATION";
    public static final String PARSE_ERROR_NOTIFICATION = "PARSE_ERROR_NOTIFICATION";
    public static final String NO_FLAGS_VISIBLE = "you won't be able to see any flags with these settings";
    public static final String SERVICE_CONNECTED = "SERVICE_CONNECTED";
    private static final String TAG = "LocationService";
    private static final long ONE_MIN = 1000 * 60;
    private static final long ONE_HOUR = ONE_MIN * 60;
    private static final long FLAG_IN_CACHE_MIN = ONE_HOUR * 2;
    private static final long INTERVAL = ONE_MIN * 5;
    private static final long FASTEST_INTERVAL = ONE_MIN * 3;
    private static final long UPDATE_CACHE_MIN_INTERVAL = ONE_MIN * 15;
    private static final int KM_TO_M = 1000;
    private static final int NOTIFICATION_ID = 12345;
    private static LocationRequest locationRequest;
    private static GoogleApiClient googleApiClient;
    private static FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;
    private final IBinder mBinder = new LocalBinder();
    private Location location;
    private ILocationUpdater listener;

    private HashMap<String, Long> cachedFlags;

    private ArrayList<Flag> flagsNearby;
    private ArrayList<Flag> hiddenFlags;

    private ArrayList<Flag> myFlags;

    private ArrayList<Flag> bagFlags;

    //we store only one suspended request
    private Integer suspendedRequest;

    private Location notificationLocation;

    private boolean noFlagsWarning;

    private long lastCacheUpdate;

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

    /**
     * update all the location-sensitive data in the application
     */
    public void updateLocationData(){
        Location currentLocation = fusedLocationProviderApi.getLastLocation(googleApiClient);
        if (currentLocation != null) {
            this.location = currentLocation;
            queryParsewithLocation(currentLocation);
            queryParsewithCurrentUser();
            queryParsewithBag();
        }else{
            suspendedRequest = PlacesUtils.DEFAULT_FLAGS_CODE;
            Log.d(TAG, "Last location is null!");
        }
    }

    /**
     * update the location-sensitive data related to updateCode
     * @param updateCode the code that identifies the location-sensitive
     *                   context we are interested in
     */
    public void updateLocationData(int updateCode){
        switch (updateCode){

            case PlacesUtils.NEARBY_FLAGS_CODE:
                Location currentLocation = fusedLocationProviderApi.getLastLocation(googleApiClient);
                if (currentLocation != null) {
                    this.location = currentLocation;
                    queryParsewithLocation(currentLocation);
                }
                else{
                    suspendedRequest = updateCode;
                    Log.d(TAG, "Last location is null!");
                }
                break;

            case PlacesUtils.MY_FLAGS_CODE:
                queryParsewithCurrentUser();
                break;

            case PlacesUtils.BAG_FLAGS_CODE:
                queryParsewithBag();
                break;

            case PlacesUtils.DEFAULT_FLAGS_CODE:
                updateLocationData();
                break;

            default:
                Log.w(TAG, "Unrecognized Flag request code. Triggering default behavior...");
                updateLocationData();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {

        Log.d(TAG, "Connected to Google Api");

        Location currentLocation = fusedLocationProviderApi.getLastLocation(googleApiClient);
        if(currentLocation != null && suspendedRequest != null){
            updateLocationData(suspendedRequest);
        }
        fusedLocationProviderApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    public void setListener(ILocationUpdater app) {
        this.listener = app;
    }

    public void resetNoFlagsWarning() {
        this.noFlagsWarning = false;
    }


    /**
     * query Parse table for all the Flags of current user
     */
    public void queryParsewithCurrentUser() {
        Log.d(TAG, "Running queryParsewithCurrentUser...");

        ParseQuery<Flag> query = ParseQuery.getQuery("Posts");
        query.whereEqualTo("fbId", PlacesLoginUtils.getInstance().getCurrentUserId());
        query.orderByDescending("createdAt");
        query.setCachePolicy(ParseQuery.CachePolicy.CACHE_THEN_NETWORK);
        query.findInBackground(new FindCallback<Flag>() {
            @Override
            public void done(List<Flag> flags, ParseException e) {
                if (e == null) {

                    if (flags == null) {
                        flags = new ArrayList<>();
                    }
                    LocationService.this.myFlags = new ArrayList<Flag>(flags);

                    updateMyFlags();

                    if (flags.size() > 0) {
                        LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.FOUND_MY_FLAGS_NOTIFICATION));
                    } else {
                        LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.FOUND_NO_MY_FLAGS_NOTIFICATION));
                    }
                }else{
                    LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.PARSE_ERROR_NOTIFICATION));
                }
            }
        });

    }

    /**
     * query Parse table for all the Flags in the current user's bag
     */
    public void queryParsewithBag() {
        Log.d(TAG, "Running queryParsewithBag...");

        final HashMap<String, Flag> filterDuplicates = new HashMap<>();

        new WoWsManager().wowsOfUser(ParseUser.getCurrentUser(), true)
                .cache(ParseQuery.CachePolicy.CACHE_THEN_NETWORK)
                .error(new ErrorCallback() {
                    @Override
                    public void error(ParseException e) {
                        LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.PARSE_ERROR_NOTIFICATION));
                    }
                })
                .success(new ModelCallback<WoWObject>() {
                    @Override
                    public void result(List<WoWObject> wows) {
                        if (wows == null)
                        {
                            wows = new ArrayList<>();
                        }
                        Log.d(TAG, "Size of wows: " + wows.size());

                        String currentUserId;

                        try
                        {
                            currentUserId = ParseUser.getCurrentUser().getObjectId();
                        }
                        catch(NullPointerException npe)
                        {
                            Log.e(TAG, npe.getMessage());
                            return;
                        }
                        for (WoWObject w : wows) {

                            Flag currFlag = w.getFlag();

                            //the following check is needed because some comments do not have a flag
                            if(currFlag == null || currFlag.getOwner() == null || currFlag.getOwner().getObjectId() == null){
                                continue;
                            }

                            String currentFlagOwner = currFlag.getOwner().getObjectId();


                            if (!currentFlagOwner.equals(currentUserId)) {
                                filterDuplicates.put(currFlag.getObjectId(), currFlag);
                            }
                        }

                        LocationService.this.bagFlags = new ArrayList<>(filterDuplicates.values());

                        Log.d(TAG, "Size of Bag: " + LocationService.this.bagFlags.size());

                        updateBagFlags();

                        if (bagFlags.size() > 0) {
                            LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.FOUND_BAG_FLAGS_NOTIFICATION));
                        } else {
                            LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.FOUND_NO_BAG_FLAGS_NOTIFICATION));
                        }
                    }
                }).start();

        new CommentsManager().commentOfUser(ParseUser.getCurrentUser(), true)
                .cache(ParseQuery.CachePolicy.CACHE_THEN_NETWORK)
                .error(new ErrorCallback() {
                    @Override
                    public void error(ParseException e) {
                        LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.PARSE_ERROR_NOTIFICATION));
                    }
                })
                .success(new ModelCallback<Comment>() {
                    @Override
                    public void result(List<Comment> comments) {
                        if (comments == null)
                        {
                            comments = new ArrayList<>();
                        }
                        Log.d(TAG, "Size of comments: " + comments.size());

                        String currentUserId;

                        try
                        {
                            currentUserId = ParseUser.getCurrentUser().getObjectId();
                        }
                        catch(NullPointerException npe)
                        {
                            Log.e(TAG, npe.getMessage());
                            return;
                        }
                        for (Comment c : comments) {

                            Flag currFlag = c.getFlag();

                            //the following check is needed because some comments do not have a flag
                            if(currFlag == null || currFlag.getOwner() == null || currFlag.getOwner().getObjectId() == null){
                                continue;
                            }

                            String currentFlagOwner = currFlag.getOwner().getObjectId();


                            if (!currentFlagOwner.equals(currentUserId)) {
                                filterDuplicates.put(currFlag.getObjectId(), currFlag);
                            }
                        }

                        LocationService.this.bagFlags = new ArrayList<>(filterDuplicates.values());

                        Log.d(TAG, "Size of Bag: " + LocationService.this.bagFlags.size());

                        updateBagFlags();

                        if (bagFlags.size() > 0) {
                            LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.FOUND_BAG_FLAGS_NOTIFICATION));
                        } else {
                            LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.FOUND_NO_BAG_FLAGS_NOTIFICATION));
                        }
                    }
                }).start();
/*
        ParseQuery<Comment> query = ParseQuery.getQuery("Comments");
        query.whereEqualTo("commenter", ParseUser.getCurrentUser());
        query.include("flag");
        query.setCachePolicy(ParseQuery.CachePolicy.CACHE_THEN_NETWORK);
        query.findInBackground(new FindCallback<Comment>() {
            @Override
            public void done(List<Comment> comments, ParseException e) {
                if (e != null) {
                    Log.e(TAG, e.getMessage());
                    Toast.makeText(getBaseContext(), "Cannot fetch your bag Flags at the moment,\ntry again later", Toast.LENGTH_SHORT).show();
                    return;
                }
                else
                {
                    if (comments == null)
                    {
                        comments = new ArrayList<>();
                    }

                    String currentUserId;

                    try
                    {
                        currentUserId = ParseUser.getCurrentUser().getObjectId();
                    }
                    catch(NullPointerException npe)
                    {
                        Log.e(TAG, npe.getMessage());
                        return;
                    }
                    HashMap<String, Flag> filterDuplicates = new HashMap<>();
                    for (Comment c : comments) {

                        Flag currFlag = c.getFlag();

                        //the following check is needed because some comments do not have a flag
                        if(currFlag == null || currFlag.getOwner() == null || currFlag.getOwner().getObjectId() == null){
                            continue;
                        }

                        String currentFlagOwner = currFlag.getOwner().getObjectId();


                        if (!currentFlagOwner.equals(currentUserId)) {
                            filterDuplicates.put(currFlag.getObjectId(), currFlag);
                        }
                    }
                    LocationService.this.bagFlags = new ArrayList<>(filterDuplicates.values());

                    updateBagFlags();

                    if (bagFlags.size() > 0) {
                        LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.FOUND_BAG_FLAGS_NOTIFICATION));
                    } else {
                        LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.FOUND_NO_BAG_FLAGS_NOTIFICATION));
                    }
                }
            }
        });

        */

    }

    /**
     * given location, we fetch the Flags nearby we are interested in
     *
     * @param location the location we want to find Flags nearby to
     */
    public void queryParsewithLocation(Location location) {
        Log.d(TAG, "Running queryParsewithLocation...");
        //creates a fake location for testing if it is running on simulator
        if (PlacesApplication.isRunningOnEmulator) {
            location = PlacesApplication.getInstance().getLocation();
        }

        //this is for avoiding a crash if location is null
        //the crash happens if there is no GPS data and the action range is changed
        if (location == null) {
            return;
        }

        if (flagsNearby == null) {
            flagsNearby = new ArrayList<>();
            hiddenFlags = new ArrayList<>();
        } else {
            flagsNearby.clear();
            hiddenFlags.clear();
        }

        final ParseGeoPoint gp = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        ParseQuery<Flag> query = ParseQuery.getQuery("Posts");

        float radius = PlacesUtils.DISCOVER_MODE_RADIUS; // double it for discover mode

        if (PlacesApplication.isRunningOnEmulator) {
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
        boolean music_check = preferences.getBoolean("musicCheck", true);

        int maxFlags_index = preferences.getInt("maxFetch", PlacesUtils.MAX_FLAGS_DEFAULT_INDEX);

        int maxFlags = PlacesUtils.STEP_VALUES[maxFlags_index];

        if (!PlacesLoginUtils.getInstance().hasCurrentUserId()) {
            final android.os.Handler handler = new android.os.Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!PlacesLoginUtils.getInstance().hasCurrentUserId()) {
                        handler.postDelayed(this, PlacesUtils.UPDATE_DELAY);
                    } else {
                        queryParsewithLocation(LocationService.this.location);
                    }
                }
            });
            return;
        }

        if (!thoughts_check && !fun_check && !landscape_check
                && !food_check && !none_check && !music_check) {
            if (!noFlagsWarning) {
                Toast.makeText(getApplicationContext(), "No category selected: "
                        + NO_FLAGS_VISIBLE, Toast.LENGTH_LONG).show();
                this.noFlagsWarning = true;
            }

            updateNearbyFlags();
            LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.FOUND_NO_FLAGS_NOTIFICATION));

            return;

        }

        ArrayList<String> selectedCategories = new ArrayList<>();
        if (thoughts_check) selectedCategories.add("Thoughts");
        if (fun_check) selectedCategories.add("Fun");
        if (landscape_check) selectedCategories.add("Landscape");
        if (food_check) selectedCategories.add("Food");
        // if (none_check) selectedCategories.add("None");
        if (none_check) selectedCategories.add("#www2015");
        if (music_check) selectedCategories.add("Music");
        query.whereContainedIn("category", selectedCategories);

        if (!storytellers_in_the_dark) {
            if (lone_wolf && with_friends_surrounded) {
                ArrayList<String> meAndMyFriends = new ArrayList<>();
                meAndMyFriends.add(PlacesLoginUtils.getInstance().getCurrentUserId());
                meAndMyFriends.addAll(PlacesLoginUtils.getInstance().getFriends());
                query.whereContainedIn("fbId", meAndMyFriends);
            } else if (lone_wolf)
                query.whereEqualTo("fbId", PlacesLoginUtils.getInstance().getCurrentUserId());
            else if (with_friends_surrounded)
                query.whereContainedIn("fbId", PlacesLoginUtils.getInstance().getFriends());
            else {
                if (!noFlagsWarning) {
                    Toast.makeText(getApplicationContext(), "No filter selected: "
                            + NO_FLAGS_VISIBLE, Toast.LENGTH_LONG).show();
                    this.noFlagsWarning = true;
                }

                if (flagsNearby == null) {
                    flagsNearby = new ArrayList<>();
                    hiddenFlags = new ArrayList<>();
                } else {
                    flagsNearby.clear();
                    hiddenFlags.clear();
                }

                updateNearbyFlags();

                LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.FOUND_NO_FLAGS_NOTIFICATION));

                return;
            }
        } else {
            if (!lone_wolf)
                query.whereNotEqualTo("fbId", PlacesLoginUtils.getInstance().getCurrentUserId());
            if (!with_friends_surrounded)
                query.whereNotContainedIn("fbId", PlacesLoginUtils.getInstance().getFriends()); // this is rather expensive
        }

        if (archaeologist) query.orderByAscending("createdAt");
        else query.orderByDescending("createdAt");

        Log.d(TAG, "Max flags: " + maxFlags);
        query.setLimit(maxFlags);

        if (PlacesApplication.isRunningOnEmulator) {
            query.setLimit(50);
        }


        query.findInBackground(new FindCallback<Flag>() {
            @Override
            public void done(List<Flag> flags, ParseException e) {
                if(e == null) {
                    if (flags == null) {
                        flags = new ArrayList<>();
                    }
                    List<Flag> hiddenFlags = new ArrayList<>();
                    Iterator<Flag> iterParseObjects = flags.iterator();
                    while (iterParseObjects.hasNext()) {
                        Flag currFlag = iterParseObjects.next();
                        Log.d(TAG, "Distance from point to user: " + currFlag.getLocation().distanceInKilometersTo(gp));
                        if (currFlag.getLocation().distanceInKilometersTo(gp) > PlacesUtils.MAP_RADIUS) {
                            hiddenFlags.add(currFlag);
                            iterParseObjects.remove();
                        }
                    }
                    for (Flag f : flags) {
                        LocationService.this.flagsNearby.add(f);
                    }
                    for (Flag f : hiddenFlags) {
                        LocationService.this.hiddenFlags.add(f);
                    }
                    updateNearbyFlags();

                    if (flags.size() > 0 || hiddenFlags.size() > 0) {
                        LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.FOUND_NEW_FLAGS_NOTIFICATION));
                        if(flags.size() > 0){
                            checkNotificationRequirements();
                        }
                    } else {
                        LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.FOUND_NO_FLAGS_NOTIFICATION));
                    }
                }else{
                    LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.PARSE_ERROR_NOTIFICATION));
                }

            }
        });
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient connection has failed " + connectionResult.toString());
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location changed");
//        Log.d(TAG, "Location accuracy: " + location.getAccuracy());
        if (notificationLocation != null && location.distanceTo(this.notificationLocation) > (PlacesUtils.MAP_RADIUS * KM_TO_M)) {
            NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nManager.cancel(NOTIFICATION_ID);
        }
        this.location = location;
        updateLocation();
        queryParsewithLocation(location);
    }

    /**
     * check if we can notify the user about Flags around. the reason why we could not,
     * for example, could be that Places is not in background, that the user has already
     * checked out the Flags around, notifications are disabled, no Flags are around, etc...
     */
    private void checkNotificationRequirements() {
        if (this.flagsNearby != null && PlacesLoginUtils.getInstance().hasCurrentUserId()) {

            //if Places is in foreground, we don't notify the user
            if (!MainActivity.isForeground()) {

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                boolean areNotificationsEnabled = preferences.getBoolean("notificationsCheck", true);

                if(areNotificationsEnabled) {
                    Log.d(TAG, "Notifying user..." + this.flagsNearby.size() + " flags found");
                    notifyUser();
                }
            } else {
                Log.d(TAG, "Main Activity in foreground: updating map...");
                LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(new Intent(LocationService.LOCATION_CHANGED_NOTIFICATION));
            }
        }
    }

    /**
     * takes care of handling the user notifications.
     * Called when new Flags are found
     */
    private void notifyUser() {
        //flags that we have never seen or haven't seen in a long time...
        int newFlags = updateCachedPostsAndRet();
        //...and we notify the user only if there are such flags
        if (newFlags > 0) {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            int numFlags = this.flagsNearby.size();
            String textIfOne = "A flag for you!";
            String textMoreThanOne = "Hey, " + this.flagsNearby.size() + " new Flags around!";

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this)
                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.app_ico_small)
                            .setContentTitle(Notifications.notifications[(int) (Math.random() * Notifications.notifications.length)])
                            .setContentText(numFlags > 1 ? textMoreThanOne : textIfOne)
                            .setSound(soundUri)
                            .setLights(0xff00ff00, 1000, 3000);

            Intent targetIntent = new Intent(this, MainActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(contentIntent);
            NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nManager.notify(NOTIFICATION_ID, builder.build());
            this.notificationLocation = this.location;
        } else {
            Log.w(TAG, "Notification to user aborted: no new Flags found: " + newFlags);
        }
    }

    /**
     * update the cache containing the Flags we have met already
     * in the close past. Returns the number of new Flags met and
     * puts them into cache
     *
     * @return the number of Flags met that are not stored in cache
     */
    private int updateCachedPostsAndRet() {
        long newTimestamp = new java.util.Date().getTime();

        if (newTimestamp - this.lastCacheUpdate > UPDATE_CACHE_MIN_INTERVAL) {
            removeCachedPosts();
        }

        int nonCachedFlags = flagsNearby.size();

        Set<String> cachedKeys = this.cachedFlags.keySet();

        for (Flag f : flagsNearby) {
            if (cachedKeys.contains(f.getObjectId())) {
                //Flag already cached.
                nonCachedFlags--;
            } else {
                //Not cached yet. Being cached now.
                this.cachedFlags.put(f.getObjectId(), newTimestamp);
            }
        }

        return nonCachedFlags;
    }

    /**
     * removes the Flags from cache whose cache timer is expired
     */
    private void removeCachedPosts() {
        this.lastCacheUpdate = new java.util.Date().getTime();
        for (String tf : this.cachedFlags.keySet()) {
            if (this.lastCacheUpdate - cachedFlags.get(tf) > FLAG_IN_CACHE_MIN) {
                this.cachedFlags.remove(tf);
            }
        }
    }

    /**
     * updates the user location and the Flags that appear in to the user
     */
    private void updateApplication() {
        if (listener != null) {
            listener.setLocation(location);
            listener.setFlagsNearby(flagsNearby);
            listener.setHiddenFlags(hiddenFlags);
            listener.setMyFlags(myFlags);
            listener.setBagFlags(bagFlags);
        }
    }

    private void updateNearbyFlags(){
        if (listener != null) {
            listener.setFlagsNearby(flagsNearby);
            listener.setHiddenFlags(hiddenFlags);
        }
    }

    private void updateLocation(){
        if (listener != null) {
            listener.setLocation(location);
        }
    }

    private void updateMyFlags(){
        if (listener != null) {
            listener.setMyFlags(myFlags);
        }
    }

    private void updateBagFlags(){
        if (listener != null) {
            listener.setBagFlags(bagFlags);
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        cachedFlags = new HashMap<>();

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(this.receiver, new IntentFilter(MainActivity.PREFERENCES_CHANGED_NOTIFICATION));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(this.receiver, new IntentFilter(CategoriesFragment.ENABLE_ALL_CLICKED));

        connectToGoogleAPI();
    }

    /**
     * exploit the Google Play Services for fetching data
     * related to user location
     */
    private void connectToGoogleAPI() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setSmallestDisplacement(PlacesUtils.MAP_RADIUS * KM_TO_M / 2);

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
    public void onDestroy() {
        Log.d(TAG, "Being Destroyed");
        super.onDestroy();
        googleApiClient.disconnect();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(this.receiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public LocationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocationService.this;
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {

                case MainActivity.PREFERENCES_CHANGED_NOTIFICATION:
                    //no automatic refresh of application data on preference change
                    break;
                case CategoriesFragment.ENABLE_ALL_CLICKED:
                    LocationService.this.resetNoFlagsWarning();
                    updateLocationData();
                    break;

                default:
                    Log.w(LocationService.class.getName(), intent.getAction() + ": cannot identify the received notification");
            }
        }
    };
}