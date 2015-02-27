package com.gcw.sapienza.places.utils;

import android.content.Context;

import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by mic_head on 27/02/15.
 */
public class GPlusUtils {

    private static final String TAG = "GPlusUtils";

    private static final GPlusUtils shared_instance = new GPlusUtils();

    private GoogleApiClient mGoogleApiClient;

    private GPlusUtils() {}

    /**
     * This is a singleton class. This method returns the ONLY instance
     *
     * @return Singleton instance
     */
    public static GPlusUtils getInstance() {
        return GPlusUtils.shared_instance;
    }

    public static void downloadGPlusInfo(Context context)
    {

    }

    public GoogleApiClient getGoogleApiClient()
    {
        return mGoogleApiClient;
    }

    public void setGoogleApiClient(GoogleApiClient googleApiClient)
    {
        this.mGoogleApiClient = googleApiClient;
    }
}
