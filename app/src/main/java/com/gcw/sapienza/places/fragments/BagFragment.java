/*
 * Copyright 2015-present Places®.
 */

package com.gcw.sapienza.places.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.FlagsStorage;
import com.gcw.sapienza.places.utils.PlacesUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * the Bag will show all flags wowed and commented
 */

public class BagFragment extends PlacesMapListFragment{
    private static final String TAG = "BagFragment";

    /**
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    @Override
    protected  void onFlagsReceived(Context context, Intent intent) {
        Log.d(TAG, "Broadcast intent received: " + intent.getAction());
    }

    @Override
    protected void onNoFlagsReceived(Context context, Intent intent) {
        Log.d(TAG, "Broadcast intent received: " + intent.getAction());
    }

    @Override
    protected String noFlagsReceivedText() {
        return "No Flags in your Bag (yet!) :(";
    }

    /**
     * This method is automatically called when the class needs to register to the local notification system.
     *
     */
    @Override
    protected  Collection<IntentFilter> getFlagsFilters() {
        ArrayList<IntentFilter> list = new ArrayList<>(1);
        list.add(new IntentFilter(LocationService.FOUND_BAG_FLAGS_NOTIFICATION));
        return list;
    }

    /**
     * This method is automatically called when the class needs to register to the local notification system.
     *
     */
    @Override
    protected  Collection<IntentFilter> getNoFlagsFilters() {
        ArrayList<IntentFilter> list = new ArrayList<>(1);
        list.add(new IntentFilter(LocationService.FOUND_NO_BAG_FLAGS_NOTIFICATION));
        return list;
    }

    /**
     * This method is called when new data is needed
     */
    @Override
    protected  void handleRefreshData() {
        ((MainActivity) getActivity()).refresh(PlacesUtils.BAG_FLAGS_CODE);
    }

    /**
     * Returns the list of flags for current subclass
     * @return list of flags
     */
    @Override
    protected  List<Flag> getData(){
        return FlagsStorage.getSharedStorage().getOrderedFlags(getActivity(), FlagsStorage.Type.BAG );
    }

    @Override
    protected boolean showDiscoverModeOnMap(){
        return false;
    }

}
