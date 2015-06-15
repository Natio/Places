package com.gcw.sapienza.places;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import com.gcw.sapienza.places.models.Comment;
import com.gcw.sapienza.places.models.CommentReport;
import com.gcw.sapienza.places.models.WoWObject;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.models.FlagReport;
import com.gcw.sapienza.places.models.PlacesUser;
import com.gcw.sapienza.places.services.ILocationServiceListener;
import com.gcw.sapienza.places.services.ILocationUpdater;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.services.LocationService.LocalBinder;
import com.gcw.sapienza.places.utils.FlagsStorage;
import com.gcw.sapienza.places.utils.PlacesUtils;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.squareup.picasso.Picasso;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;


public class PlacesApplication extends Application {
    public static final boolean isRunningOnEmulator = Build.BRAND.toLowerCase().startsWith("generic");
    //just the tag for logging
    private static final String TAG = "PlacesApplication";
    //Parse.com app key
    private static final String PARSE_COM_APP_KEY = "BWtqN9x6uyr935MKAROcWkc6mzv8KLQMMVnFGHps";

    //Parse.com client key
    private static final String PARSE_COM_CLIENT_KEY = "Gr1g8Z2kfv3AOZqToZ30hyMyNzH24vj4yudNoKfb";
    //the app context
    private static Context PLACES_CONTEXT = null;
    private static PlacesApplication placesApplication;
    //current location
    private Location cachedLocation;

    private HashMap<ILocationServiceListener, Integer> serviceListeners = new HashMap<>();
    private ILocationUpdater listener = new ILocationUpdater() {

        @Override
        public void setFlagsNearby(Collection<Flag> l) {
            if (l != null) FlagsStorage.getSharedStorage().storeFlagsWithType(l, FlagsStorage.Type.NEARBY);
        }

        @Override
        public void setHiddenFlags(Collection<Flag> l) {
            if (l != null) FlagsStorage.getSharedStorage().storeFlagsWithType(l, FlagsStorage.Type.HIDDEN);
        }

        @Override
        public void setMyFlags(Collection<Flag> myFlags) {
            if (myFlags != null) FlagsStorage.getSharedStorage().storeFlagsWithType(myFlags, FlagsStorage.Type.MY);
        }

        @Override
        public void setBagFlags(Collection<Flag> bagFlags) {
            if (bagFlags != null) FlagsStorage.getSharedStorage().storeFlagsWithType(bagFlags, FlagsStorage.Type.BAG);
        }
    };
    private LocationService mService;
    @SuppressWarnings("UnusedDeclaration")
    private boolean mBound = false;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mService.setListener(listener);
            PlacesApplication.this.mBound = true;
            Iterator<ILocationServiceListener> iter = serviceListeners.keySet().iterator();
            while(iter.hasNext()){
                ILocationServiceListener currListener = iter.next();
                if(currListener != null) {
                    Log.d(TAG, "onServiceConnected, firing callback on: " + currListener.getClass());
                    currListener.onServiceConnected(serviceListeners.get(currListener));
                }
                iter.remove();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            PlacesApplication.this.mBound = false;
        }
    };


    /**
     * Call this method to access the UNIQUE PlacesApplication instance
     *
     * @return The unique instance of PlacesApplication
     */
    public static PlacesApplication getInstance() {
        return PlacesApplication.placesApplication;
    }

    /**
     * @return App context
     */
    @SuppressWarnings("unused")
    public static Context getPlacesAppContext() {
        return PlacesApplication.PLACES_CONTEXT;
    }

    public boolean isLocationServiceRunning(){ return this.mBound; }

    private static void subscribeToParseBroadcast() {
        ParsePush.subscribeInBackground("", new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d("com.parse.push", "successfully subscribed to the broadcast channel.");
                } else {
                    // Log.e("com.parse.push", "failed to subscribe for push", e);
                    Log.e(TAG, "failed to subscribe for push");
                }
            }
        });
    }

    /**
     * @return returns LocationService instance
     */
    @SuppressWarnings("unused declaration")
    public LocationService getLocationService() {
        return this.mService;
    }



    /**
     * Returns the current location if available. If running on emulator this method
     * will return a fake position somewhere in the middle of Rome.
     *
     * @return see description
     */
    public Location getLocation() {
        //if (/*PlacesApplication.isRunningOnEmulator &&*/ this.currentLocation == null) {
        if (PlacesApplication.isRunningOnEmulator && this.cachedLocation == null) {

            Location loc = new Location("rome_center");
            loc.setLatitude(41.900193);
            loc.setLongitude(12.472916);
            this.cachedLocation = LocationService.getRandomLocation(loc, 1000);
        }
        Location currentLocation = mService.getCurrentLocation();
        if(currentLocation != null){
            this.cachedLocation = currentLocation;
        }
        return this.cachedLocation;
    }

    /*
    public Flag getFlagWithId(String id) {
        if (this.flagsNearby.get(id) != null)
            return this.flagsNearby.get(id);
        else if (this.myFlags.get(id) != null)
            return this.myFlags.get(id);
        else if (this.hiddenFlags.get(id) != null)
            return this.hiddenFlags.get(id);
        else if (this.bagFlags.get(id) != null)
            return this.bagFlags.get(id);
        else {
            Utils.showToast(PLACES_CONTEXT, "There was a problem retrieving Flag data", Toast.LENGTH_SHORT);
            return null;
        }
    }*/

    //method called when the app is launched
    @Override
    public void onCreate() {
        super.onCreate();

        PlacesApplication.PLACES_CONTEXT = this.getApplicationContext();
        PlacesApplication.placesApplication = this;

        if (BuildConfig.DEBUG) {
            Picasso.with(this).setIndicatorsEnabled(false); // if in debug show color indicators on pictures
        }


        //register subclasses of Parse objects
        ParseObject.registerSubclass(Flag.class);
        ParseObject.registerSubclass(FlagReport.class);
        ParseObject.registerSubclass(WoWObject.class);
        ParseObject.registerSubclass(Comment.class);
        ParseUser.registerSubclass(PlacesUser.class);
        ParseUser.registerSubclass(CommentReport.class);

        // initialize Parse.com
        Parse.initialize(this, PARSE_COM_APP_KEY, PARSE_COM_CLIENT_KEY);
        ParseFacebookUtils.initialize(getString(R.string.app_id));

//        PushService.setDefaultPushCallback(this, MainActivity.class);


        //Parse push notifications
        ParsePush.subscribeInBackground("Developers"); //TODO developers channel, remove for user version
        subscribeToParseBroadcast();


        String android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        boolean hasToSaveInstallation = false;
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        String installationUniqueId = (String) installation.get("uniqueId");

        if (installationUniqueId == null || !installationUniqueId.equals(android_id)) {
            ParseInstallation.getCurrentInstallation().put("uniqueId", android_id);
            hasToSaveInstallation = true;
        }


        ParseUser owner = (ParseUser) ParseInstallation.getCurrentInstallation().get("owner");
        if (ParseUser.getCurrentUser() != null && owner != null) {
            if (ParseUser.getCurrentUser().getObjectId().equals(owner.getObjectId()))
                ParseInstallation.getCurrentInstallation().put("owner", ParseUser.getCurrentUser());
            hasToSaveInstallation = true;
        }
        if (hasToSaveInstallation) {
            ParseInstallation.getCurrentInstallation().saveInBackground();
        }

    }

    /**
     * start the location service that provides updates on the location-sensitive
     * data in Places. Upon connection, it automatically fetches location-sensitive
     * data related to the Home page
     */
    public void startLocationService() {
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Intent locInt = new Intent(this, LocationService.class);
            Log.d(TAG, "Starting Location Service");
            //        stopService(locInt);
            startService(locInt);
            bindService(locInt, this.mConnection, BIND_AUTO_CREATE);
        } else {
            Log.w(TAG, "Location Service not started!");
        }
    }

    /**
     * update location-sensitive data in Places. If the LocationService is not
     * yet started, start it instead.
     * @param serviceListener the listener of location-sensitive data updates
     * @return true if the location service was started already, false otherwise
     */
    public boolean updatePlacesData(ILocationServiceListener serviceListener){
        if(mService != null){
            mService.updateLocationData();
            return true;
        } else {
            serviceListeners.put(serviceListener, PlacesUtils.DEFAULT_FLAGS_CODE);
            startLocationService();
            return false;
        }
    }

    /**
     * update location-sensistive data in Places related to a particular context
     * If the LocationService is not yet started, start it instead.
     * @param serviceListener the listener of location-sensitive data updates
     * @param updateCode the update context we are interested in
     * @return true if the location service was started already, false otherwise
     */
    public boolean updatePlacesData(ILocationServiceListener serviceListener, int updateCode){
        if(mService != null) {
            mService.updateLocationData(updateCode);
            return true;
        } else {
            serviceListeners.put(serviceListener, updateCode);
            startLocationService();
            return false;
        }
    }

/*

ALERT ALERT ALERT ALERT
DO NOT CALL THE FOLLOWING METHOD UNLESS YOU REALLY WANT TO COUNT ALL COMMENTS (IT IS REALLY EXPENSIVE)


    private void countComments(){
        ParseQuery<Flag> q = ParseQuery.getQuery("Posts");
        q.setLimit(1000);
        q.whereDoesNotExist(Flag.COMMENTS_COUNT_KEY);
        q.findInBackground(new FindCallback<Flag>() {
            @Override
            public void done(final List<Flag> flags, ParseException e) {
                if(e != null){
                    Log.e(TAG, "ERRORREERRRRRRRR**",e);
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for(int i = 0; i< flags.size(); i++){
                            System.gc();

                            Flag f = flags.get(i);
                            ParseQuery<Comment> countQuery = ParseQuery.getQuery("Comments");
                            countQuery.whereEqualTo("flagId", f.getFlagId());
                            try{
                                int count = countQuery.count();
                                f.put(Flag.COMMENTS_COUNT_KEY, count);
                                f.save();
                                Log.d(TAG, "Salvo "+(i+1)+" su "+ flags.size());
                                SystemClock.sleep(100);
                            }
                            catch(ParseException e){
                                Log.e(TAG, "ERRORE", e);
                                return;
                            }


                        }
                    }
                }).start();


            }
        });
    }



    ALERT ALERT ALERT ALERT
    DO NOT CALL THE FOLLOWING METHOD UNLESS YOU REALLY WANT TO GENERATE ALL THE THUMBNAILS



    private void generateThumbnails(){
        ParseQuery<Flag> query = ParseQuery.getQuery("Posts");
        query.setLimit(1000);
        query.whereDoesNotExist(Flag.THUMBNAIL_KEY);
        query.whereExists(Flag.PICTURE_KEY);
        query.findInBackground(new FindCallback<Flag>() {
            @Override
            public void done(final List<Flag> flags, ParseException e) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            for(int i = 0; i < flags.size(); i++){
                                System.gc();
                                final int index = i;
                                final Flag f = flags.get(i);
                                Log.d(TAG, "Processing "+i + " over"+ flags.size());
                                //if(f.getThumbnail() == null) continue;

                                ParseFile pic = f.getPic();
                                if(pic != null){
                                    byte[] data = pic.getData();
                                    Bitmap bmp;
                                    bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                                    Bitmap result = BitmapUtils.createThumbnailForImageRespectingProportions(bmp);
                                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                    result.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                                    byte[] byteArray = stream.toByteArray();
                                    final ParseFile thumb_f = new ParseFile("thumb"+Utils.generateRandomName()+".jpg", byteArray);
                                    thumb_f.save();
                                    bmp.recycle();;
                                    bmp = null;
                                    result.recycle();
                                    result = null;
                                    f.setThumbnailFile(thumb_f);
                                    f.save();
                                    Log.d(TAG, "Saved "+index + " over"+ flags.size());
                                    byteArray = null;
                                }


                            }
                        }catch(ParseException e){
                            Log.d(TAG, e.toString(),e);
                        }


                    }
                }).start();


            }
        });
    }
*/
}
