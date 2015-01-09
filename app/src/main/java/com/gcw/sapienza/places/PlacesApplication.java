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

import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.services.ILocationUpdater;
import com.gcw.sapienza.places.services.LocationService;
import com.parse.ConfigCallback;
import com.parse.Parse;
import com.parse.ParseConfig;
import com.parse.ParseFacebookUtils;
import com.parse.ParseObject;
import com.parse.ParseException;
import com.gcw.sapienza.places.services.LocationService.LocalBinder;
//Parse push notifications
import com.parse.ParsePush;
import com.parse.SaveCallback;
import com.squareup.picasso.Picasso;

import java.io.IOException;
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
    private static Location currentLocation = null;

    //TODO I do not know if this gives a false positive on a real device. If it does just assign false
    public static final boolean isRunningOnEmulator = Build.BRAND.toLowerCase().startsWith("generic");



    private static List<Flag> pinsNearby;


    public static LocationService mService;
    boolean mBound = false;

    private static String locality;

    //shared variable for handling weather conditions
    private static String weather = "";

    public static PlacesApplication placesApplication;

    /**
     *
     * @return string representing weather conditions
     */
    public static String getWeather(){
        return PlacesApplication.weather;
    }

    /**
     * Sets the weather
     * @param weather string representing the weather
     */
    public static void setWeather(String weather){
        PlacesApplication.weather = weather;
    }

    //made synchronized for thread safety and added fake location if running on emulator
    public static synchronized Location getLocation(){
        if (PlacesApplication.isRunningOnEmulator && currentLocation == null) {

            Location loc = new Location("rome_center");
            loc.setLatitude(41.900193);
            loc.setLongitude(12.472916);
            currentLocation = LocationService.getRandomLocation(loc, 1000);
        }
        return currentLocation;
    }

    public static List<Flag> getPins(){
        return pinsNearby;
    }

    public static Context getPlacesAppContext(){
        return PlacesApplication.PLACES_CONTEXT;
    }

    //method called when the app is launched
    @Override
    public void onCreate() {
        super.onCreate();


        PlacesApplication.PLACES_CONTEXT = this.getApplicationContext();

        placesApplication = this;

        if(BuildConfig.DEBUG){
            Picasso.with(this).setIndicatorsEnabled(true); // if in debug show color indicators on pictures
        }


        // initialize Parse.com
        ParseObject.registerSubclass(Flag.class);
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
        subscribeToParseBroadcast();

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

    public void startLocationService() {
        LocationManager locationManager = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            Intent locInt = new Intent(this, LocationService.class);
            Log.d("Places Application", "Starting Location Service");
    //        stopService(locInt);
            startService(locInt);
            bindService(locInt, mConnection, BIND_AUTO_CREATE);
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
            Log.d(TAG, "*******************************"+mService+"");
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            mBound = false;
        }
    };

    private ILocationUpdater listener = new ILocationUpdater() {
        public void setLocation(Location l){
            PlacesApplication.currentLocation = l;
            updateWeatherInfo();
        }
        public void setPinsNearby(List<Flag> l){
            PlacesApplication.pinsNearby = l;
        }
    };

    private void updateWeatherInfo() {
        Geocoder gcd = new Geocoder(this, Locale.getDefault());
        try {

            Location current = PlacesApplication.getLocation();

            List<Address> addresses = gcd.getFromLocation(current.getLatitude(), current.getLongitude(), 1);
            if (addresses.size() > 0) {
                Log.d(TAG, "Locality: " + addresses.get(0).getLocality());
                locality = addresses.get(0).getLocality();
                String cc = addresses.get(0).getCountryCode();
                JSONWeatherTask task = new JSONWeatherTask();
                task.execute(new String[]{locality + "," + cc});
            }
        }catch (IOException e){
            Log.e(TAG, "No locality found! Error: " + e.toString());
        }
    }

}
