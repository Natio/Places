package com.gcw.sapienza.places.fragments;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.FlagsStorage;
import com.gcw.sapienza.places.utils.FlagsStorage.Type;
import com.gcw.sapienza.places.utils.PlacesUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by snowblack on 2/19/15.
 */

public class MainFragment extends PlacesMapListFragment{
    private static final String TAG = "MainFragment";

    /**
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    @Override
    protected  void onFlagsReceived(Context context, Intent intent) {
        Log.d(TAG, "Broadcast intent received: " + intent.getAction());
    }

    /**
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    @Override
    protected void onNoFlagsReceived(Context context, Intent intent) {
        Log.d(TAG, "Broadcast intent received: " + intent.getAction());
    }

    /**
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    @Override
    protected void onParseError(Context context, Intent intent) {
        Log.d(TAG, "Broadcast intent received: " + intent.getAction());
    }

    @Override
    protected String noFlagsReceivedText() {
        return "No Flags nearby (yet!) :(";
    }

    /**
     * This method is automatically called when the class needs to register to the local notification system.
     *
     */
    @Override
    protected Collection<IntentFilter> getFlagsFilters() {
        ArrayList<IntentFilter> list = new ArrayList<>(3);
        list.add(new IntentFilter(LocationService.FOUND_NEW_FLAGS_NOTIFICATION));
        list.add(new IntentFilter(LocationService.LOCATION_CHANGED_NOTIFICATION));
        return list;
    }

    @Override
    protected Collection<IntentFilter> getNoFlagsFilters() {
        ArrayList<IntentFilter> list = new ArrayList<>(1);
        list.add(new IntentFilter(LocationService.FOUND_NO_FLAGS_NOTIFICATION));
        return list;
    }

    /**
     * This method is called when new data is needed
     */
    @Override
    protected  void handleRefreshData() {
        MainActivity mainActivity = (MainActivity) getActivity();
        if(mainActivity != null) {
            mainActivity.refresh(PlacesUtils.NEARBY_FLAGS_CODE);
        }
    }

    /**
     * Returns the list of flags for current subclass
     * @return list of flags
     */
    @Override
    protected  List<Flag> getData(){
        return FlagsStorage.getSharedStorage().fetchFlagsWithType(Type.NEARBY);
    }

    @Override
    protected boolean showDiscoverModeOnMap(){
        return true;
    }

    @Override
    protected Requirements fragmentRequirements() {
        return Requirements.ALL;
    }

    protected boolean mapZoomsAroundSearchRadius(){
        return true;
    }
}