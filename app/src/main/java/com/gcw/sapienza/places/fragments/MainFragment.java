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
import com.gcw.sapienza.places.utils.Utils;


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
    protected  void onBroadcastReceived(Context context, Intent intent) {
        Log.d(TAG, "Intent broadcast ricevuto: " + intent.getAction());
    }

    /**
     * This method is automatically called when the class needs to register to the local notification system.
     *
     */
    @Override
    protected Collection<IntentFilter> getNotificationFilters() {
        ArrayList<IntentFilter> list = new ArrayList<>(2);
        list.add(new IntentFilter(LocationService.FOUND_NEW_FLAGS_NOTIFICATION));
        list.add(new IntentFilter(LocationService.FOUND_NO_FLAGS_NOTIFICATION));
        list.add(new IntentFilter(LocationService.LOCATION_CHANGED_NOTIFICATION));
        list.add(new IntentFilter(MainActivity.PREFERENCES_CHANGED_NOTIFICATION));
        return list;
    }

    /**
     * This method is called when new data is needed
     */
    @Override
    protected  void handleRefreshData() {
        ((MainActivity) getActivity()).refresh(Utils.NEARBY_FLAGS_CODE);
    }

    /**
     * Returns the list of flags for current subclass
     * @return list of flags
     */
    @Override
    protected  List<Flag> getData(){
        return FlagsStorage.getSharedStorage().getOrderedFlags(getActivity(), Type.NEARBY );
    }

    @Override
    protected boolean showDiscoverModeOnMap(){
        return true;
    }
}