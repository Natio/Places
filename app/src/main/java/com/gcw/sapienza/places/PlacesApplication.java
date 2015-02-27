package com.gcw.sapienza.places;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.gcw.sapienza.places.models.Comment;
import com.gcw.sapienza.places.models.CustomParseObject;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.models.FlagReport;
import com.gcw.sapienza.places.services.ILocationUpdater;
import com.gcw.sapienza.places.services.JSONWeatherTask;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.services.LocationService.LocalBinder;
import com.parse.ConfigCallback;
import com.parse.Parse;
import com.parse.ParseConfig;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.SaveCallback;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class PlacesApplication extends Application{
    //just the tag for logging
    private static final String TAG = "PlacesApplication";

    //the app context
    private static Context PLACES_CONTEXT = null;

    //Parse.com app key
    private static final String PARSE_COM_APP_KEY = "BWtqN9x6uyr935MKAROcWkc6mzv8KLQMMVnFGHps";

    //Parse.com client key
    private static final String PARSE_COM_CLIENT_KEY = "Gr1g8Z2kfv3AOZqToZ30hyMyNzH24vj4yudNoKfb";

    //current location
    private Location currentLocation = null;

    public static final boolean isRunningOnEmulator = Build.BRAND.toLowerCase().startsWith("generic");

    private List<Flag> flagsNearby = new ArrayList<>(0);
    private List<Flag> myFlags = new ArrayList<>(0);

    private LocationService mService;

    @SuppressWarnings("UnusedDeclaration")
    private boolean mBound = false;

    //shared variable for handling weather conditions
    private String weather = "";

    private static PlacesApplication placesApplication;

    /**
     * Call this method to access the UNIQUE PlacesApplication instance
     * @return The unique instance of PlacesApplication
     */
    public static PlacesApplication getInstance(){
        return PlacesApplication.placesApplication;
    }


    /**
     *
     * @return returns LocationService instance
     */
    public LocationService getLocationService(){
        return this.mService;
    }

    /**
     *
     * @return string representing weather conditions
     */
    public String getWeather(){
        return this.weather;
    }

    /**
     * Sets the weather
     * @param weather string representing the weather
     */
    public void setWeather(String weather){
        this.weather = weather;
    }

    /**
     * Returns the current location if available. If running on emulator this method
     * will return a fake position somewhere in the middle of Rome.
     * @return see description
     */
    public Location getLocation(){
        //if (/*PlacesApplication.isRunningOnEmulator &&*/ this.currentLocation == null) {
        if (PlacesApplication.isRunningOnEmulator && this.currentLocation == null) {

            Location loc = new Location("rome_center");
            loc.setLatitude(41.900193);
            loc.setLongitude(12.472916);
            this.currentLocation = LocationService.getRandomLocation(loc, 1000);
        }
        return this.currentLocation;
    }

    /**
     *
     * @return returns the list of flags around user's location, filtered according to settings
     */
    public List<Flag> getFlags(){
        return this.flagsNearby;
    }

    /**
     *
     * @return returns the list of all the Flags the user has posted
     */
    public List<Flag> getMyFlags(){
        return this.myFlags;
    }

    /**
     *
     * @return App context
     */
    @SuppressWarnings("unused")
    public static Context getPlacesAppContext(){
        return PlacesApplication.PLACES_CONTEXT;
    }

    //method called when the app is launched
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "CIAO");

        PlacesApplication.PLACES_CONTEXT = this.getApplicationContext();
        PlacesApplication.placesApplication = this;

        if(BuildConfig.DEBUG){
            Picasso.with(this).setIndicatorsEnabled(false); // if in debug show color indicators on pictures
        }


        // initialize Parse.com
        ParseObject.registerSubclass(Flag.class);
        ParseObject.registerSubclass(FlagReport.class);
        ParseObject.registerSubclass(CustomParseObject.class);
        ParseObject.registerSubclass(Comment.class);
        Parse.initialize(this, PARSE_COM_APP_KEY , PARSE_COM_CLIENT_KEY);
        ParseFacebookUtils.initialize(getString(R.string.app_id));
        ParseConfig.getInBackground(new ConfigCallback() {
            @Override
            public void done(ParseConfig parseConfig, ParseException e) {
                if(e != null){
                    Log.d(TAG, "Error while configuring: "+e.getMessage());
                }
                else{
                    Log.d(TAG, "Got new Configuration");
                }
            }
        });
//        PushService.setDefaultPushCallback(this, MainActivity.class);


        //Parse push notifications
        ParsePush.subscribeInBackground("Developers"); //TODO developers channel, remove for user version
        subscribeToParseBroadcast();


        PlacesApplication.getInstance().startLocationService();
    }

    private static void subscribeToParseBroadcast() {
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

    public void startLocationService() {
        LocationManager locationManager = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            Intent locInt = new Intent(this, LocationService.class);
            Log.d("Places Application", "Starting Location Service");
    //        stopService(locInt);
            startService(locInt);
            bindService(locInt, this.mConnection, BIND_AUTO_CREATE);
        }else{
            Log.w("Places Application", "Location Service not started!");
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mService.setListener(listener);
            PlacesApplication.this.mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            PlacesApplication.this.mBound = false;
        }
    };
    private ILocationUpdater listener = new ILocationUpdater() {
        @Override
        public void setLocation(Location l){
            PlacesApplication.this.currentLocation = l;
            PlacesApplication.this.updateWeatherInfo();
        }
        @Override
        public void setFlagsNearby(List<Flag> l){
            PlacesApplication.this.flagsNearby = l;
        }

        @Override
        public void setMyFlags(List<Flag> myFlags) { PlacesApplication.this.myFlags = myFlags; }
    };

    private void updateWeatherInfo() {
        Geocoder gcd = new Geocoder(this, Locale.getDefault());
        try {

            Location current = this.currentLocation;

            List<Address> addresses = gcd.getFromLocation(current.getLatitude(), current.getLongitude(), 1);
            if (addresses.size() > 0) {
                Log.d(TAG, "Locality: " + addresses.get(0).getLocality());
                String locality = addresses.get(0).getLocality();
                String cc = addresses.get(0).getCountryCode();
                JSONWeatherTask task = new JSONWeatherTask();
                task.execute(locality + ',' + cc);
            }
        }catch (IOException e){
            Log.e(TAG, "No locality found! Error: " + e.toString());
        }
    }




/*

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
