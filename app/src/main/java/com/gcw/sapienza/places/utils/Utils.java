package com.gcw.sapienza.places.utils;


import android.content.Context;
import android.os.Environment;

import com.gcw.sapienza.places.MainActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Created by mic_head on 02/01/15.
 */
public class Utils
{
    @SuppressWarnings("unused")
    private static final String TAG = "Utils";

    public static final int UPDATE_DELAY = 200;
    public static float MAP_RADIUS = 0.5f;
    public static int MAX_PINS = 10;

    public static int[] stepValues = {1, 5, 10, 15, 20};

    public static final int VID_SHOOT_REQUEST_CODE = 90;
    public static final int PIC_CAPTURE_REQUEST_CODE = 91;

    public static final int SETTINGS_REQUEST_CODE = 92;
    public static final int GPS_ENABLE_REQUEST_CODE = 93;
    public static final int LOGIN_REQUEST_CODE = 94;

    public static final int VIBRATION_DURATION = 0; // I didn't like it that much

    public static final int DELETE_POST = 0;
    public static final int REPORT_POST = 1;
    public static final int REMOVE_REPORT_POST = 2;

    protected static final int CHUNK_SIZE = 4096;

    public static String[] categories;

    public static MainActivity mainActivity;

/*
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
*/

    public static byte[] convertFileToByteArray(File f) throws IOException{
        FileInputStream stream = new FileInputStream(f);
        byte [] res = Utils.convertStreamToByteArray(stream);
        stream.close();
        return res;
    }

    public static byte[] convertStreamToByteArray(FileInputStream is) throws IOException
    {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        byte[] buff = new byte[CHUNK_SIZE];
        int i = Integer.MAX_VALUE;

        while ((i = is.read(buff, 0, buff.length)) > 0)
        {
            outStream.write(buff, 0, i);
        }

        return outStream.toByteArray();
    }

    private static String generateRandomName(){
        return "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + "_";
    }

    public static File createAudioFile(String extension, Context ctx) throws IOException{

        String imageFileName = "AUDIO" + Utils.generateRandomName();

        File cache_dir = ctx.getExternalCacheDir();
        return File.createTempFile(
                imageFileName,
                extension,
                cache_dir
        );


    }

    public static File createImageFile(String image_extension) throws IOException {
        // Create an image file name
        String imageFileName = "IMG" + Utils.generateRandomName();
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                image_extension,         /* suffix */
                storageDir      /* directory */
        );
    }

}
