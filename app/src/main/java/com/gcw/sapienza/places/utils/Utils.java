package com.gcw.sapienza.places.utils;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.widget.Toast;

import com.gcw.sapienza.places.MainActivity;


/**
 * Created by mic_head on 02/01/15.
 */
public class Utils
{
    private static final String TAG = "Utils";

    public static final int UPDATE_DELAY = 200;
    public static float MAP_RADIUS = 0.5f;
    public static int MAX_PINS = 10;

    public static int[] stepValues = {1, 5, 10, 15, 20};

    public static final int PIC_CAPTURE_REQUEST_CODE = 91;
    public static final int SETTINGS_REQUEST_CODE = 92;
    public static final int GPS_ENABLE_REQUEST_CODE = 93;
    public static final int LOGIN_REQUEST_CODE = 94;

    public static final int VIBRATION_DURATION = 0; // I didn't like it that much

    public static final int DELETE_POST = 0;
    public static final int REPORT_POST = 1;
    public static final int REMOVE_REPORT_POST = 2;

    public static String[] categories;

    public static MainActivity mainActivity;


    @Deprecated // Daniele says its 'getLocation' is much better than mine, that's why it's deprecated.
    @SuppressWarnings("unused")
    public static Location getLocation(Context context)
    {
        Location location;
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (isGPSEnabled || isNetworkEnabled)
        {
            if (isNetworkEnabled)
            {
                if (locationManager != null)
                {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    return location;
                }
            }
            if (isGPSEnabled)
            {
                if (locationManager != null)
                {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    return location;
                }
            }
        }
        else{
            Toast.makeText(context, "Please enable GPS data", Toast.LENGTH_LONG).show();
        }

        return null;
    }
}
