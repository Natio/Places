/*
 * Copyright 2015-present Places®.
 */

package com.gcw.sapienza.places.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.adapters.FlagsAdapter;
import com.gcw.sapienza.places.layouts.MSwipeRefreshLayout;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.models.FlagReport;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.FlagsStorage;
import com.gcw.sapienza.places.utils.PlacesUtils;
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
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Abstract class for showing Fragment with Map and list of flags.
 * Subclasses must implement all abstract methods providing the right data.
 * Paolo++ ;)
 */

public abstract class PlacesMapListFragment extends Fragment implements OnMapReadyCallback, SwipeRefreshLayout.OnRefreshListener,
        GoogleMap.OnMarkerClickListener {

    private static final String TAG = "PlacesMapListFragment";
    public static PlacesMapListFragment currentFragmentInstance;

    public static PlacesMapListFragment getInstance() {
        return PlacesMapListFragment.currentFragmentInstance;
    }

    public enum Requirements {NONE, NETWORK, LOCATION, ALL}

    ;

    private GoogleMap gMap;
    private List<Marker> markers;
    protected FlagsListFragment listFragment;
    private MSwipeRefreshLayout srl;
    private SlidingUpPanelLayout supl;


    private BroadcastReceiver flagsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            PlacesMapListFragment.this.onFlagsReceived(context, intent);
            PlacesMapListFragment.this.updateMarkersOnMap();
            List<Flag> data = PlacesMapListFragment.this.getData();
            PlacesMapListFragment.this.listFragment.flagsToDisplay();
            PlacesMapListFragment.this.listFragment.updateRecycleViewWithNewContents(data);
        }
    };

    private BroadcastReceiver noFlagsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            PlacesMapListFragment.this.onNoFlagsReceived(context, intent);
            PlacesMapListFragment.this.listFragment.noFlagsToDisplay(noFlagsReceivedText());
        }
    };

    private BroadcastReceiver parseErrorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            PlacesMapListFragment.this.onNoFlagsReceived(context, intent);
            hintMissingRequirements(Requirements.NETWORK);
        }
    };

    protected abstract String noFlagsReceivedText();

    /**
     * @param context The Context in which the flagsReceiver is running.
     * @param intent  The Intent being received.
     */
    protected abstract void onFlagsReceived(Context context, Intent intent);

    /**
     * @param context The Context in which the noFlagsReceiver is running.
     * @param intent  The Intent being received.
     */
    protected abstract void onNoFlagsReceived(Context context, Intent intent);

    /**
     * @param context The Context in which the parseErrorReceiver is running.
     * @param intent  The Intent being received.
     */
    protected abstract void onParseError(Context context, Intent intent);

    /**
     * This method is automatically called when the class needs to register to the local notification system.
     */
    protected abstract Collection<IntentFilter> getFlagsFilters();


    /**
     * This method is automatically called when the class needs to register to the local notification system.
     */
    protected abstract Collection<IntentFilter> getNoFlagsFilters();

    /**
     * This method is called when new data is needed
     */
    protected abstract void handleRefreshData();

    /**
     * Returns the list of flags for current subclass
     *
     * @return list of flags
     */
    protected abstract List<Flag> getData();


    /**
     * @return true if you want to enable discover mode on map
     */
    protected abstract boolean showDiscoverModeOnMap();

    /**
     * @return code indicating the network/location requirements of the fragment
     */
    protected abstract Requirements fragmentRequirements();

    @Override
    public void onRefresh() {
        this.handleRefreshData();
        srl.setRefreshing(false);
    }

    public void refresh(){
        this.handleRefreshData();
    }

    /**
     * Registers the flagsReceiver from local notifications
     */
    private void registerNotification() {
        Collection<IntentFilter> flagsFilters = this.getFlagsFilters();
        for (IntentFilter filter : flagsFilters) {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(this.flagsReceiver, filter);
        }
        Collection<IntentFilter> noFlagsFilters = this.getNoFlagsFilters();
        for (IntentFilter filter : noFlagsFilters) {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(this.noFlagsReceiver, filter);
        }
        registerParseErrorNotifications();
    }

    private void registerParseErrorNotifications() {
        IntentFilter parseErrorFilter = new IntentFilter(LocationService.PARSE_ERROR_NOTIFICATION);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(this.parseErrorReceiver, parseErrorFilter);
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    /**
     * Unregisters the flagsReceiver from local notifications
     */
    private void unregisterNotification() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(this.flagsReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(this.noFlagsReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(this.parseErrorReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.registerNotification();
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.my_flags_layout, container, false);
        currentFragmentInstance = this;

        this.srl = (MSwipeRefreshLayout) view.findViewById(R.id.my_swipe_refresh);
        this.srl.setOnRefreshListener(this);
        this.srl.setOnChildScrollUpListener(new MSwipeRefreshLayout.OnChildScrollUpListener() {
            @Override
            public boolean canChildScrollUp() {
                FlagsListFragment f = PlacesMapListFragment.this.listFragment;
                if (f == null) return false;
                RecyclerView rv = f.getRecyclerView();
                if (rv == null) return false;
                RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();

                int position = ((LinearLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition();
                /*if(position == 0 || supl.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)
                {
                    // if(supl.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) supl.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
                    if(supl.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED) supl.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    else if(supl.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED || supl.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN)
                        supl.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
                    return true;
                }*/

                return position != 0 && rv.getAdapter().getItemCount() != 0;
            }
        });
        this.supl = (SlidingUpPanelLayout) view.findViewById(R.id.home_container);

        //temporarily disabled anchorpoint
        //this.supl.setAnchorPoint(0.5f);

        //to disable drag function
        // this.supl.setTouchEnabled(false);
        this.supl.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
        this.supl.setDragView(R.id.drag_handler);
        // this.supl.setAnchorPoint(0.75f);

        this.supl.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View view, float v) {
                // Log.d(TAG, "Panel sliding");
            }

            @Override
            public void onPanelCollapsed(View view) {
                Log.d(TAG, "Panel collapsed");

                //made temporarily scrollable
                //gMap.getUiSettings().setScrollGesturesEnabled(true);
                // supl.setTouchEnabled(true);
                ((ImageView) view.findViewById(R.id.arrowView)).setImageResource(R.drawable.arrow_down);
            }

            @Override
            public void onPanelExpanded(View view) {
                Log.d(TAG, "Panel expanded");
                //made temporarily scrollable
                //gMap.getUiSettings().setScrollGesturesEnabled(true);
                // supl.setClickable(false);
                // supl.setTouchEnabled(false);
                // supl.setEnableDragViewTouchEvents(false);
                // supl.setLongClickable(false);

                ((ImageView) view.findViewById(R.id.arrowView)).setImageResource(R.drawable.arrow_up);


            }

            @Override
            public void onPanelAnchored(View view) {
                Log.d(TAG, "Panel anchored");

                ((ImageView) view.findViewById(R.id.arrowView)).setImageResource(R.drawable.arrow_up);
            }

            @Override
            public void onPanelHidden(View view) {
                Log.d(TAG, "Panel hidden");
            }
        });

        this.listFragment = new FlagsListFragment();
        this.listFragment.setInitialData(this.getData());
        this.listFragment.setSUPL(supl);
        this.listFragment.setSRL(srl);
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

    /**
     * Customizes GMap UI. Subclasses can implement this method to change the UiSettings of GMaps
     * When implementing in subclasses remember to call super.customizeGmap()
     *
     * @param googleMap the Gmap
     */
    protected void customizeGmap(GoogleMap googleMap) {
        googleMap.getUiSettings().setScrollGesturesEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.setOnMarkerClickListener(this);
        googleMap.setMyLocationEnabled(true);
    }

    /**
     * Called when GMap is ready
     *
     * @param googleMap GMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.gMap = googleMap;
        this.customizeGmap(googleMap);

        this.handleRefreshData();

        Requirements requirements = fragmentRequirements();

        hintMissingRequirements(requirements);
    }

    private void hintMissingRequirements(Requirements requirements) {
        MainActivity mainActivity = (MainActivity) getActivity();

        boolean networkReady = mainActivity.isNetworkAvailable();
        boolean locationReady = mainActivity.areLocationServicesEnabled();

        if (requirements == Requirements.NONE) {
            PlacesMapListFragment.this.listFragment.flagsToDisplay();
        } else {
            switch (requirements) {
                case NETWORK:
                    if (!networkReady) {
                        PlacesMapListFragment.this.listFragment.noFlagsToDisplay("No Internet connection available :(");
                    }
                    break;
                case LOCATION:
                    if (!locationReady) {
                        PlacesMapListFragment.this.listFragment.noFlagsToDisplay("No location data available :(");
                    }
                    break;
                case ALL:
                    if (!networkReady && locationReady) {
                        PlacesMapListFragment.this.listFragment.noFlagsToDisplay("No Internet connection and location data available :(");
                    } else if (networkReady && !locationReady) {
                        PlacesMapListFragment.this.listFragment.noFlagsToDisplay("No location data available :(");
                    } else if (!networkReady && !locationReady) {
                        PlacesMapListFragment.this.listFragment.noFlagsToDisplay("No Internet connection and location data available :(");
                    }
                    break;
                default:
                    Log.w(TAG, "Unknown missing requirement: " + requirements);
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker selectedMarker)
    {
        String snippet = selectedMarker.getSnippet();
        Log.d(TAG, "Marker Snippet: " + snippet);

        if (snippet != null) {
            int index = Integer.parseInt(selectedMarker.getSnippet());

            this.listFragment.getRecyclerView().smoothScrollToPosition(index);

            if (selectedMarker.getAlpha() == PlacesUtils.FLAG_ALPHA_SELECTED) {
                for (Marker marker : this.markers) {
                    marker.setAlpha(PlacesUtils.FLAG_ALPHA_NORMAL);
                }
            } else {
                for (Marker marker : this.markers) {
                    marker.setAlpha(PlacesUtils.FLAG_ALPHA_UNSELECTED);
                }
                selectedMarker.setAlpha(PlacesUtils.FLAG_ALPHA_SELECTED);
            }
        } else {
            //Doesn't work properly. Most likely Google Maps API Bug
            if (selectedMarker.isInfoWindowShown()) {
                selectedMarker.hideInfoWindow();
            } else {
                selectedMarker.showInfoWindow();
            }
        }

        // by returning false we can show text on flag in the map
        // return false;

        this.supl.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

        return true;
    }

    // Puts markers on the map
    private void updateMarkersOnMap() {

        this.markers = new ArrayList<>();

        List<Flag> flags = this.getData();

        Log.d(TAG, "Updating markers on map...flags size: " + flags.size());

        if (this.gMap != null) {
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
                int marker_id = PlacesUtils.getIconForCategory(f.getCategory(), getActivity());
                Bitmap marker = BitmapFactory.decodeResource(getResources(), marker_id);
                Bitmap halfSizeMarker = Bitmap.createScaledBitmap
                        (marker,
                                (int) (marker.getWidth() * PlacesUtils.FLAG_SCALE_NORMAL),
                                (int) (marker.getHeight() * PlacesUtils.FLAG_SCALE_NORMAL),
                                false);

                Marker newMarker = this.gMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(text)
                        .snippet(String.valueOf(index))
                        .icon(BitmapDescriptorFactory.fromBitmap(halfSizeMarker))
                                // .icon(BitmapDescriptorFactory.fromResource(getIconForCategory(f.getCategory())))
                                //.icon(BitmapDescriptorFactory.defaultMarker(getCategoryColor(f.getCategory())))
                        .alpha(PlacesUtils.FLAG_ALPHA_NORMAL));
                this.markers.add(newMarker);
                index++;
            }

            if (this.showDiscoverModeOnMap()) {

                List<Flag> hiddenFlags = FlagsStorage.getSharedStorage().fetchFlagsWithType(FlagsStorage.Type.HIDDEN);

                for (ParseObject hf : hiddenFlags) {
                    Flag f = (Flag) hf;
                    ParseGeoPoint location = f.getLocation();
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    builder.include(latLng);

                    //25% size original icon
                    int marker_id = PlacesUtils.getIconForCategory(f.getCategory(), getActivity());
                    Bitmap marker = BitmapFactory.decodeResource(getResources(), marker_id);
                    Bitmap halfSizeMarker = Bitmap.createScaledBitmap
                            (marker,
                                    (int) (marker.getWidth() * PlacesUtils.FLAG_SCALE_NORMAL),
                                    (int) (marker.getHeight() * PlacesUtils.FLAG_SCALE_NORMAL),
                                    false);

                    this.gMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title("Get closer to see the content!")
                            .icon(BitmapDescriptorFactory.fromBitmap(halfSizeMarker))
                                    // .icon(BitmapDescriptorFactory.fromResource(getIconForCategory(f.getCategory())))
                                    //.icon(BitmapDescriptorFactory.defaultMarker(getCategoryColor(f.getCategory())))
                            .alpha(0.25f));
                }
            }

            Location currentLocation = gMap.getMyLocation() != null ?
                    gMap.getMyLocation() : PlacesApplication.getInstance().getLocation();

            if (currentLocation != null) {
                LatLng currentLocationLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                builder.include(currentLocationLatLng);
                this.gMap.addCircle(new CircleOptions()
                        .center(currentLocationLatLng)
                        .radius(PlacesUtils.MAP_RADIUS * 1000)
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
                    gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, PlacesUtils.MAP_BOUNDS));
                } catch (IllegalStateException ise) {
                    this.gMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                        @Override
                        public void onCameraChange(CameraPosition cameraPosition) {
                            gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, PlacesUtils.MAP_BOUNDS));
//                            gMap.setOnCameraChangeListener(null);
                        }
                    });
                }
            } else {
                if (currentLocation != null) {
                    LatLng currentLocationLatLng = new LatLng(currentLocation.getLatitude(),
                            currentLocation.getLongitude());
                    this.gMap.animateCamera(CameraUpdateFactory
                            .newLatLngZoom(currentLocationLatLng, PlacesUtils.ZOOM_LVL));
                }
            }

        }
    }

    /**
     * Fragment that shows the list of flags in a cardview
     */
    public static class FlagsListFragment extends Fragment {

        private RecyclerView recycleView;
        private RelativeLayout noFlagLayout;
        private TextView noFlagsText;
        private SlidingUpPanelLayout supl;
        private MSwipeRefreshLayout srl;

        public void setSUPL(SlidingUpPanelLayout supl) {
            this.supl = supl;
        }

        public void setSRL(MSwipeRefreshLayout srl) {
            this.srl = srl;
        }

        //the following variable is used to fill the recycle view at creation time, after onCreateView it will be null. Do not use it
        private List<Flag> initialData;

        protected void flagsToDisplay() {
            noFlagLayout.setVisibility(View.GONE);
            recycleView.setVisibility(View.VISIBLE);
        }

        protected void noFlagsToDisplay(String text) {
            noFlagsText.setText(text);
            recycleView.setVisibility(View.GONE);
            noFlagLayout.setVisibility(View.VISIBLE);
        }

        public void setInitialData(List<Flag> initialData) {
            this.initialData = initialData;
        }

        public RecyclerView getRecyclerView() {
            return recycleView;
        }


        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.flags_list_new_layout, container, false);

            this.recycleView = (RecyclerView) view.findViewById(R.id.cardList);
            this.recycleView.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);

                    /*
                    int firstVisibleItemPos = ((LinearLayoutManager)recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                    if(firstVisibleItemPos == 0)
                    {
                        FlagsListFragment.this.supl.setTouchEnabled(true);
                        FlagsListFragment.this.srl.setClickable(false);
                        FlagsListFragment.this.srl.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                return false;
                            }
                        });
                    }
                    */
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                }
            });

            LinearLayoutManager llm = new LinearLayoutManager(this.getActivity());
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            this.recycleView.setLayoutManager(llm);

            noFlagLayout = (RelativeLayout) view.findViewById(R.id.no_flags_found_layout);
            noFlagsText = (TextView) noFlagLayout.findViewById(R.id.no_flags_text);


            if (this.initialData != null) {
                this.updateRecycleViewWithNewContents(this.initialData);
                this.initialData = null;
            }

            registerForContextMenu(recycleView);

            return view;
        }

        public void updateRecycleViewWithNewContents(List<Flag> l) {
            this.recycleView.setAdapter(new FlagsAdapter(l, recycleView, getActivity()));
            this.recycleView.scrollToPosition(((MainActivity)getActivity()).flagClicked);
        }


        @Override
        public boolean onContextItemSelected(MenuItem item) {
            if(item.getGroupId() == PlacesUtils.FLAG_LIST_GROUP) {
                FlagsAdapter fa = (FlagsAdapter) recycleView.getAdapter();
                Flag sel_usr = fa.getSelectedFlag();

                if (sel_usr == null)
                    Toast.makeText(getActivity(), PlacesUtils.NO_VALID_FLAG_SELECTED, Toast.LENGTH_SHORT).show();

                switch (item.getItemId()) {

                    case PlacesUtils.DELETE_FLAG:
                        this.deleteFlag(sel_usr);
                        fa.setSelectedFlagIndex(-1);
                        return true;

                    case PlacesUtils.REPORT_FLAG:
                        this.reportFlag(sel_usr);
                        fa.setSelectedFlagIndex(-1);
                        return true;

                    case PlacesUtils.DELETE_REPORT_FLAG:
                        this.deleteReportFlag(sel_usr);
                        fa.setSelectedFlagIndex(-1);
                        return true;

                    default:
                        fa.setSelectedFlagIndex(-1);
                        return super.onContextItemSelected(item);
                }
            }else{
                Log.w(TAG, "Not handling the context item selection in " + TAG);
                return false;
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
                        Toast.makeText(recycleView.getContext(), PlacesUtils.FLAG_DELETED, Toast.LENGTH_SHORT).show();
//                        ((MainActivity) getActivity()).refresh(PlacesUtils.NEARBY_FLAGS_CODE);
                        PlacesMapListFragment.getInstance().handleRefreshData();
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
                        Toast.makeText(recycleView.getContext(), PlacesUtils.FLAG_REPORTED, Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(recycleView.getContext(), PlacesUtils.FLAG_REPORT_REVOKED, Toast.LENGTH_SHORT).show();
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
