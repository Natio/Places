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

/*
 * My FlagsFragment, looks like the homepage but it's possible to see all flags we own around the
 * world
 */

public class MyFlagsFragment extends PlacesMapListFragment {
    private static final String TAG = "MyFlagsFragment";

    /**
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    protected void onFlagsReceived(Context context, Intent intent) {
        Log.d(TAG, "Broadcast intent received: " + intent.getAction());
    }

    /**
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    protected void onNoFlagsReceived(Context context, Intent intent) {
        Log.d(TAG, "Broadcast intent received: " + intent.getAction());
    }

    /**
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    protected void onParseError(Context context, Intent intent) {
        Log.d(TAG, "Broadcast intent received: " + intent.getAction());
    }

    @Override
    protected String noFlagsReceivedText() {
        return "No Flags from you (yet!) :(";
    }

    /**
     * This method is automatically called when the class needs to register to the local notification system.
     */
    @Override
    protected Collection<IntentFilter> getFlagsFilters() {
        ArrayList<IntentFilter> list = new ArrayList<>(1);
        list.add(new IntentFilter(LocationService.FOUND_MY_FLAGS_NOTIFICATION));
        return list;
    }

    /**
     * This method is automatically called when the class needs to register to the local notification system.
     */
    @Override
    protected Collection<IntentFilter> getNoFlagsFilters() {
        ArrayList<IntentFilter> list = new ArrayList<>(1);
        list.add(new IntentFilter(LocationService.FOUND_NO_MY_FLAGS_NOTIFICATION));
        return list;
    }

    /**
     * This method is called when new data is needed
     */
    @Override
    protected void handleRefreshData() {
        ((MainActivity) getActivity()).refresh(PlacesUtils.MY_FLAGS_CODE);
    }

    /**
     * Returns the list of flags for current subclass
     *
     * @return list of flags
     */
    @Override
    protected List<Flag> getData() {
        return FlagsStorage.getSharedStorage().fetchFlagsWithType(FlagsStorage.Type.MY);
    }

    @Override
    protected boolean showDiscoverModeOnMap() {
        return false;
    }

    @Override
    protected Requirements fragmentRequirements() {
        return Requirements.NETWORK;
    }
}