package com.gcw.sapienza.places.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.adapters.FlagsAdapter;
import com.gcw.sapienza.places.layouts.MSwipeRefreshLayout;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.models.FlagReport;
import com.gcw.sapienza.places.utils.FlagsStorage;
import com.gcw.sapienza.places.utils.Utils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.DeleteCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by paolo on 26/03/15.
 */
public abstract class PlacesMapListFragment extends Fragment implements OnMapReadyCallback, SwipeRefreshLayout.OnRefreshListener,
        GoogleMap.OnMarkerClickListener {

    private static final String TAG = "PlacesMapListFragment";

    private GoogleMap gMap;
    private List<Marker> markers;
    private FlagsListFragment listFragment;
    private MSwipeRefreshLayout srl;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            PlacesMapListFragment.this.onBroadcastReceived(context, intent);
            PlacesMapListFragment.this.updateMarkersOnMap();
            List<Flag> data = PlacesMapListFragment.this.getData();
            PlacesMapListFragment.this.listFragment.updateRecycleViewWithNewContents(data);
        }
    };


    /**
    * @param context The Context in which the receiver is running.
    * @param intent The Intent being received.
    */
    protected abstract void onBroadcastReceived(Context context, Intent intent);

    /**
     * This method is automatically called when the class needs to register to the local notification system.
     *
     */
    protected abstract Collection<IntentFilter> getNotificationFilters();

    /**
     * This method is called when new data is needed
     */
    protected abstract void handleRefreshData();

    /**
     * Returns the list of flags for current subclass
     * @return list of flags
     */
    protected abstract List<Flag> getData();


    /**
     *
     * @return true if you want to enable discover mode on map
     */
    protected abstract boolean showDiscoverModeOnMap();


    @Override
    public void onRefresh(){
        this.handleRefreshData();
        srl.setRefreshing(false);
    }


    /**
     * Registers the receiver from local notifications
     */
    private void registerNotification(){
        Collection<IntentFilter> filters = this.getNotificationFilters();
        for(IntentFilter filter : filters){
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(this.receiver, filter);
        }
    }
    /**
     * Unregisters the receiver from local notifications
     */
    private   void unregisterNotification(){
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(this.receiver);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.registerNotification();
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.my_flags_layout, container, false);


        this.srl = (MSwipeRefreshLayout) view.findViewById(R.id.my_swipe_refresh);
        this.srl.setOnRefreshListener(this);


        this.srl.setOnChildScrollUpListener(new MSwipeRefreshLayout.OnChildScrollUpListener() {
            @Override
            public boolean canChildScrollUp() {


                FlagsListFragment f = PlacesMapListFragment.this.listFragment;
                if(f == null){
                    return false;
                }
                RecyclerView rv = f.getRecyclerView();

                if (rv == null) return false;

                RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();

                int position = ((LinearLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition();

                // Log.d(TAG, "First completely visible item position: " + position);

                return position != 0 && rv.getAdapter().getItemCount() != 0;
            }
        });


        this.listFragment = new FlagsListFragment();
        this.listFragment.setInitialData(this.getData());
        this.getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.my_swipe_refresh, this.listFragment).commit();

        SupportMapFragment mapFragment = new SupportMapFragment();
        this.getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.my_map_holder, mapFragment).commit();
        mapFragment.getMapAsync(this);

        return view;
    }

    @Override
    public void onDestroyView() {
        this.unregisterNotification();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.gMap = googleMap;
        this.gMap.getUiSettings().setScrollGesturesEnabled(true);
        this.gMap.getUiSettings().setZoomGesturesEnabled(true);
        this.gMap.setOnMarkerClickListener(this);
        this.gMap.setMyLocationEnabled(true);

        this.handleRefreshData();
    }

    @Override
    public boolean onMarkerClick(Marker selectedMarker) {
        int index = Integer.parseInt(selectedMarker.getSnippet());

        this.listFragment.getRecyclerView().smoothScrollToPosition(index);


        if(selectedMarker.getAlpha() == Utils.FLAG_ALPHA_SELECTED){
            for (Marker marker : this.markers) {
                marker.setAlpha(Utils.FLAG_ALPHA_NORMAL);
            }
        }else {
            for (Marker marker : this.markers) {
                marker.setAlpha(Utils.FLAG_ALPHA_UNSELECTED);
            }
            selectedMarker.setAlpha(Utils.FLAG_ALPHA_SELECTED);
        }

        // by returning false we can show text on flag in the map
        // return false;
        return true;
    }


    private void updateMarkersOnMap() {

        this.markers = new ArrayList<>();

        List<Flag> flags = FlagsStorage.getSharedStorage().getOrderedFlags(getActivity(), FlagsStorage.Type.NEARBY);

        Log.d(TAG, "Updating markers on map...flags size: " + flags.size());

        if ( this.gMap != null) {
            this.gMap.clear();

            //zooms around all the Flags
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            int index = 0;

            for (Flag f : flags) {
                ParseGeoPoint location = f.getLocation();
                String text = f.getText();
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                builder.include(latLng);

                //25% size original icon
                int marker_id = Utils.getIconForCategory(f.getCategory(), getActivity());
                Bitmap marker = BitmapFactory.decodeResource(getResources(), marker_id);
                Bitmap halfSizeMarker = Bitmap.createScaledBitmap
                        (marker,
                                (int) (marker.getWidth() * Utils.FLAG_SCALE_NORMAL),
                                (int) (marker.getHeight() * Utils.FLAG_SCALE_NORMAL),
                                false);

                Marker newMarker = this.gMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(text)
                        .snippet( String.valueOf(index))
                        .icon(BitmapDescriptorFactory.fromBitmap(halfSizeMarker))
                                // .icon(BitmapDescriptorFactory.fromResource(getIconForCategory(f.getCategory())))
                                //.icon(BitmapDescriptorFactory.defaultMarker(getCategoryColor(f.getCategory())))
                        .alpha(Utils.FLAG_ALPHA_NORMAL));
                this.markers.add(newMarker);
                index++;
            }

            if (this.showDiscoverModeOnMap() && isDiscoverModeEnabled()) {

                List<Flag> hiddenFlags = FlagsStorage.getSharedStorage().fetchFlagsWithType(FlagsStorage.Type.HIDDEN);

                Log.d(TAG, "Hidden Flags size: " + hiddenFlags.size());

                for (ParseObject hf : hiddenFlags) {
                    Flag f = (Flag) hf;
                    ParseGeoPoint location = f.getLocation();
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    builder.include(latLng);

                    //25% size original icon
                    int marker_id = Utils.getIconForCategory(f.getCategory(), getActivity());
                    Bitmap marker = BitmapFactory.decodeResource(getResources(), marker_id);
                    Bitmap halfSizeMarker = Bitmap.createScaledBitmap
                            (marker,
                                    (int) (marker.getWidth() * Utils.FLAG_SCALE_NORMAL),
                                    (int) (marker.getHeight() * Utils.FLAG_SCALE_NORMAL),
                                    false);

                    this.gMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title("**DiscoverMode**\nget closer to see the content")
                            .icon(BitmapDescriptorFactory.fromBitmap(halfSizeMarker))
                                    // .icon(BitmapDescriptorFactory.fromResource(getIconForCategory(f.getCategory())))
                                    //.icon(BitmapDescriptorFactory.defaultMarker(getCategoryColor(f.getCategory())))
                            .alpha(0.25f));
                }
            }

            Location currentLocation = gMap.getMyLocation();

            if (currentLocation != null) {
                LatLng currentLocationLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                builder.include(currentLocationLatLng);
                this.gMap.addCircle(new CircleOptions()
                        .center(currentLocationLatLng)
                        .radius(Utils.MAP_RADIUS * 1000)
                        .strokeColor(Color.GREEN)
                        .fillColor(Color.TRANSPARENT));
            }

            if (flags.size() > 0) {
                final LatLngBounds bounds = builder.build();
//                this.gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Utils.MAP_BOUNDS));
                // FIXME That's a mess, Paolo forgive me
                /*if (PlacesLoginUtils.loginType == PlacesLoginUtils.LoginType.GPLUS)
                    this.gMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                        @Override
                        public void onCameraChange(CameraPosition cameraPosition) {
                            gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Utils.MAP_BOUNDS));
                            gMap.setOnCameraChangeListener(null);
                        }
                    });
                else*/
                try {
                    gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Utils.MAP_BOUNDS));
                } catch (IllegalStateException ise) {
                    this.gMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                        @Override
                        public void onCameraChange(CameraPosition cameraPosition) {
                            gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Utils.MAP_BOUNDS));
//                            gMap.setOnCameraChangeListener(null);
                        }
                    });
                }
            } else {
                if (currentLocation != null) {
                    LatLng currentLocationLatLng = new LatLng(currentLocation.getLatitude(),
                            currentLocation.getLongitude());
                    this.gMap.animateCamera(CameraUpdateFactory
                            .newLatLngZoom(currentLocationLatLng, Utils.ZOOM_LVL));
                }
            }

        }
    }

    /**
     * check if discover mode is enabled
     *
     * @return true if discover mode is enabled, false otherwise
     */
    private boolean isDiscoverModeEnabled() {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(PlacesApplication.getPlacesAppContext());

        boolean discoverMode = preferences.getBoolean("discoverMode", true);

        return discoverMode;
    }


    public static class FlagsListFragment extends Fragment{

        private RecyclerView recycleView;

        //the following variable is used to fill the recycle view at cration time, after onCreateView it will be null. Do not use it
        private List<Flag> initialData;



        public void setInitialData(List<Flag> initialData){
            this.initialData = initialData;
        }

        public RecyclerView getRecyclerView() {
            return recycleView;
        }


        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.flags_list_new_layout, container, false);

            this.recycleView = (RecyclerView) view.findViewById(R.id.cardList);
            LinearLayoutManager llm = new LinearLayoutManager(this.getActivity());
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            this.recycleView.setLayoutManager(llm);


            if(this.initialData != null){
                this.updateRecycleViewWithNewContents(this.initialData);
                Log.d(TAG, "Metto initial data");
                this.initialData = null;
            }

            registerForContextMenu(recycleView);

            return view;
        }

        public void updateRecycleViewWithNewContents(List<Flag> l) {
            this.recycleView.setAdapter(new FlagsAdapter(l, recycleView, getActivity()));
        }


        @Override
        public boolean onContextItemSelected(MenuItem item) {
            FlagsAdapter fa = (FlagsAdapter) recycleView.getAdapter();
            Flag sel_usr = fa.getSelectedFlag();

            if (sel_usr == null)
                Toast.makeText(getActivity(), Utils.NO_VALID_FLAG_SELECTED, Toast.LENGTH_SHORT).show();

            switch (item.getItemId()) {

                case Utils.DELETE_FLAG:
                    this.deleteFlag(sel_usr);
                    fa.setSelectedFlagIndex(-1);
                    return true;

                case Utils.REPORT_FLAG:
                    this.reportFlag(sel_usr);
                    fa.setSelectedFlagIndex(-1);
                    return true;

                case Utils.DELETE_REPORT_FLAG:
                    this.deleteReportFlag(sel_usr);
                    fa.setSelectedFlagIndex(-1);
                    return true;

                default:
                    fa.setSelectedFlagIndex(-1);
                    return super.onContextItemSelected(item);
            }
        }

        /**
         * Deletes the comment
         *
         * @param f flag to delete
         */
        private void deleteFlag(Flag f) {
            f.deleteInBackground(new DeleteCallback() {
                @Override
                public void done(com.parse.ParseException e) {
                    if (e == null) {
                        Toast.makeText(recycleView.getContext(), Utils.FLAG_DELETED, Toast.LENGTH_SHORT).show();
                        ((MainActivity) getActivity()).refresh(Utils.NEARBY_FLAGS_CODE);
                    } else
                        Toast.makeText(recycleView.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**
         * Reports a flag
         *
         * @param f flag to report
         */
        private void reportFlag(final Flag f) {

            FlagReport report = FlagReport.createFlagReportFromFlag(f);
            report.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        Toast.makeText(recycleView.getContext(), Utils.FLAG_REPORTED, Toast.LENGTH_SHORT).show();
                    } else
                        Toast.makeText(recycleView.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**
         * Deletes an entry from the Reported_Posts table
         *
         * @param f flag related to the entry to be deleted
         */
        private void deleteReportFlag(Flag f) {
            ParseQuery<ParseObject> queryDelete = ParseQuery.getQuery("Reported_Posts");

            queryDelete.whereEqualTo("reported_by", ParseUser.getCurrentUser());
            queryDelete.whereEqualTo("reported_flag", f);

            queryDelete.getFirstInBackground(new GetCallback<ParseObject>() {
                @Override
                public void done(ParseObject p, ParseException e) {
                    if (e == null) {
                        p.deleteInBackground(new DeleteCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null) {
                                    Toast.makeText(recycleView.getContext(), Utils.FLAG_REPORT_REVOKED, Toast.LENGTH_SHORT).show();
                                } else
                                    Toast.makeText(recycleView.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(recycleView.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }



    }


}
