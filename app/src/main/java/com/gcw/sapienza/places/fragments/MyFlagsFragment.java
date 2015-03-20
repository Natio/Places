package com.gcw.sapienza.places.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.layouts.MSwipeRefreshLayout;
import com.gcw.sapienza.places.models.Flag;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.Utils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by snowblack on 2/19/15.
 */
public class MyFlagsFragment extends Fragment implements OnMapReadyCallback, SwipeRefreshLayout.OnRefreshListener,
        GoogleMap.OnMarkerClickListener {

    private static final String TAG = "MyFlagsFragment";

    private View view;
    private GoogleMap gMap;
    private BroadcastReceiver receiver;
    private RelativeLayout progressBarHolder;
    private MSwipeRefreshLayout srl;
    private FragmentActivity myContext;
    private List<Flag> flags;
    private TextView progressTextView;
    private DrawerLayout drawerLayout;
    private LinearLayout homeHolder;
    private FrameLayout fragHolder;

    private List<Marker> markers;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                /**
                 * eventually, we might want to have different behaviors
                 * for different events (whether my flags have been found or not)
                 */
                switch (intent.getAction()) {

                    case LocationService.FOUND_MY_FLAGS_NOTIFICATION:

                        Log.d(TAG, "My flags found");
//                        MyFlagsFragment.this.dismissProgressBar();
                        break;

                    case LocationService.FOUND_NO_MY_FLAGS_NOTIFICATION:

                        Log.d(TAG, "No my flags found");
//                        MyFlagsFragment.this.dismissProgressBar();
                        break;

                    default:
                }
                updateMarkersOnMap();
            }
        };

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(this.receiver, new IntentFilter(LocationService.FOUND_MY_FLAGS_NOTIFICATION));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(this.receiver, new IntentFilter(LocationService.FOUND_NO_MY_FLAGS_NOTIFICATION));
    }

    @Override
    public void onAttach(Activity activity) {
        this.myContext = (FragmentActivity) activity;
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        view = inflater.inflate(R.layout.my_flags_layout, container, false);

        this.progressBarHolder = (RelativeLayout) view.findViewById(R.id.frame_layout);
        this.progressTextView = (TextView) view.findViewById(R.id.share_progress_text_view);

        this.homeHolder = (LinearLayout) view.findViewById(R.id.my_home_container);
        this.fragHolder = (FrameLayout) view.findViewById(R.id.my_frag_container);

        srl = (MSwipeRefreshLayout) view.findViewById(R.id.my_swipe_refresh);
        srl.setOnRefreshListener(this);

//        this.homeHolder.setVisibility(View.INVISIBLE);
//        this.fragHolder.setVisibility(View.INVISIBLE);
//        this.srl.setVisibility(View.INVISIBLE);

        srl.setOnChildScrollUpListener(new MSwipeRefreshLayout.OnChildScrollUpListener() {
            @Override
            public boolean canChildScrollUp() {
                List<Fragment> frags = myContext.getSupportFragmentManager().getFragments();

                if (frags.size() < 1) return false;

                RecyclerView rv = null;

                for (int i = 0; i < frags.size(); i++) {
                    if (frags.get(i) instanceof MyFlagsListFragment) {
                        rv = ((MyFlagsListFragment) frags.get(i)).getRV();
                        break;
                    }
                }

                if (rv == null) return false;

                RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();

                int position = ((LinearLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition();

                // Log.d(TAG, "First completely visible item position: " + position);

                return position != 0 && rv.getAdapter().getItemCount() != 0;
            }
        });

//        showProgressBar();

        Fragment fragment = new MyFlagsListFragment();
        myContext.getSupportFragmentManager().beginTransaction().replace(R.id.my_swipe_refresh, fragment).commit();

        SupportMapFragment mapFragment = new SupportMapFragment();
        myContext.getSupportFragmentManager().beginTransaction().replace(R.id.my_map_holder, mapFragment).commit();
        mapFragment.getMapAsync(MyFlagsFragment.this);

        return view;
    }

    private void showProgressBar() {
        AlphaAnimation inAnim = new AlphaAnimation(0, 1);
        inAnim.setDuration(Utils.ANIMATION_DURATION);
        progressBarHolder.setAnimation(inAnim);
        progressBarHolder.setVisibility(View.VISIBLE);
    }

    private void dismissProgressBar() {

        AlphaAnimation outAnim = new AlphaAnimation(1, 0);
        outAnim.setDuration(Utils.ANIMATION_DURATION);
        progressBarHolder.setAnimation(outAnim);
        progressBarHolder.setVisibility(View.GONE);

//                MyFlagsFragment.this.homeHolder.setVisibility(View.VISIBLE);
//                MyFlagsFragment.this.fragHolder.setVisibility(View.VISIBLE);
//                MyFlagsFragment.this.srl.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(this.receiver);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        this.gMap = googleMap;

        this.gMap.getUiSettings().setScrollGesturesEnabled(true);
        this.gMap.getUiSettings().setZoomGesturesEnabled(true);
        this.gMap.setOnMarkerClickListener(this);
        this.gMap.setMyLocationEnabled(true);

        this.updateMarkersOnMap();
    }

    @Override
    public boolean onMarkerClick(Marker selectedMarker) {

        int index = Integer.parseInt(selectedMarker.getSnippet());

        List<Fragment> frags = getActivity().getSupportFragmentManager().getFragments();

        if (frags.size() < 1) return false;

        for (int i = 0; i < frags.size(); i++) {
            if (frags.get(i) instanceof MyFlagsListFragment) {
                MyFlagsListFragment flf = ((MyFlagsListFragment) frags.get(i));
                flf.getRV().smoothScrollToPosition(index);
                // TODO item highlight on flag clicked on map?

                break;
            }
        }

        for(Marker marker: this.markers){
            marker.setAlpha(0.85f);
        }
        selectedMarker.setAlpha(1f);

        // by returning false we can show text on flag in the map
        // return false;
        return true;
    }

    private void updateMarkersOnMap() {

        this.flags = PlacesApplication.getInstance().getMyFlags();

        this.markers = new ArrayList<>();

        if (this.flags != null && this.gMap != null) {
            this.gMap.clear();

            //zooms around all the Flags
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            int index = 0;

            for (ParseObject p : this.flags) {
                Flag f = (Flag) p;
                ParseGeoPoint location = f.getLocation();
                String text = f.getText();
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                builder.include(latLng);

                //25% size original icon
                int marker_id = Utils.getIconForCategory(f.getCategory(), getActivity());
                Bitmap marker = BitmapFactory.decodeResource(getResources(), marker_id);
                Bitmap halfSizeMarker = Bitmap.createScaledBitmap
                        (marker,
                                (int) (marker.getWidth() * 0.25f),
                                (int) (marker.getHeight() * 0.25f),
                                false);

                Marker currMarker = this.gMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(text)
                        .snippet(index + "")
                        .icon(BitmapDescriptorFactory.fromBitmap(halfSizeMarker))
                                // .icon(BitmapDescriptorFactory.fromResource(getIconForCategory(f.getCategory())))
                                //.icon(BitmapDescriptorFactory.defaultMarker(getCategoryColor(f.getCategory())))
                        .alpha(0.85f));

                this.markers.add(currMarker);

                index++;
            }

            if (this.flags.size() > 0) {
                LatLngBounds bounds = builder.build();
//                this.gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Utils.MAP_BOUNDS));
                this.gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Utils.MAP_BOUNDS));
            } else {
                Location currentLocation = gMap.getMyLocation();
                if (currentLocation != null) {
                    LatLng currentLocationLatLng = new LatLng(currentLocation.getLatitude(),
                            currentLocation.getLongitude());
                    this.gMap.animateCamera(CameraUpdateFactory
                            .newLatLngZoom(currentLocationLatLng, Utils.ZOOM_LVL));
                }
            }
        }
    }

    @Override
    public void onRefresh() {
        refresh();
        srl.setRefreshing(false);
    }

    protected void refresh() {
        PlacesApplication.getInstance().getLocationService().queryParsewithCurrentUser();
    }
}