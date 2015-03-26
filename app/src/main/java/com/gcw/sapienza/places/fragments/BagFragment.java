package com.gcw.sapienza.places.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.FlagsStorage;
import com.gcw.sapienza.places.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by snowblack on 3/20/15.
 */

public class BagFragment extends PlacesMapListFragment{
    private static final String TAG = "BagFragment";

    /**
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    @Override
    protected  void onBroadcastReceived(Context context, Intent intent) {
        Log.d(TAG, "Intent broadcast ricevuto: " + intent.getAction());
    }

    /**
     * This method is automatically called when the class needs to register to the local notification system.
     *
     */
    @Override
    protected  Collection<IntentFilter> getNotificationFilters() {
        ArrayList<IntentFilter> list = new ArrayList<>(2);
        list.add(new IntentFilter(LocationService.FOUND_BAG_FLAGS_NOTIFICATION));
        list.add(new IntentFilter(LocationService.FOUND_NO_BAG_FLAGS_NOTIFICATION));
        return list;
    }

    /**
     * This method is called when new data is needed
     */
    @Override
    protected  void handleRefreshData() {
        ((MainActivity) getActivity()).refresh(Utils.BAG_FLAGS_CODE);
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
