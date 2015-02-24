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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.MainActivity;
import com.gcw.sapienza.places.layouts.MSwipeRefreshLayout;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.Utils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.util.List;

/**
 * Created by snowblack on 2/19/15.
 */
public class MainFragment extends Fragment implements OnMapReadyCallback, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "MainFragment";

    private View view;

    private GoogleMap gMap;

    private BroadcastReceiver receiver;

    private RelativeLayout progressBarHolder;
    private TextView progressTextView;

    private DrawerLayout drawerLayout;
    private LinearLayout homeHolder;
    private FrameLayout fragHolder;
    private MSwipeRefreshLayout srl;

    private FragmentActivity myContext;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                /**
                 * eventually, we might want to have different behaviors
                 * for different events (whether my flags have been found or not)
                 */
                switch(intent.getAction()) {

                    case LocationService.FOUND_NEW_FLAGS_NOTIFICATION:

                        Log.d(TAG, "My flags found");
//                        this.dismissProgressBar();
                        break;

                    case LocationService.FOUND_NO_FLAGS_NOTIFICATION:

                        Log.d(TAG, "No my flags found");
//                        this.dismissProgressBar();
                        break;

                    default:
                }
                updateMarkersOnMap();
            }

//            void dismissProgressBar(){
//
//                AlphaAnimation outAnim = new AlphaAnimation(1, 0);
//                outAnim.setDuration(Utils.ANIMATION_DURATION);
//                progressBarHolder.setAnimation(outAnim);
//                progressBarHolder.setVisibility(View.GONE);
//
//                MyFlagsFragment.this.homeHolder.setVisibility(View.VISIBLE);
//                MyFlagsFragment.this.fragHolder.setVisibility(View.VISIBLE);
//                MyFlagsFragment.this.srl.setVisibility(View.VISIBLE);
//            }
        };

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(this.receiver, new IntentFilter(LocationService.FOUND_NEW_FLAGS_NOTIFICATION));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(this.receiver, new IntentFilter(LocationService.FOUND_NO_FLAGS_NOTIFICATION));
    }

    @Override
    public void onAttach(Activity activity){
        this.myContext=(FragmentActivity) activity;
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);

        view = inflater.inflate(R.layout.my_flags_layout, container, false);

//        this.progressBarHolder = (RelativeLayout)view.findViewById(R.id.frame_layout);
//        this.progressTextView = (TextView)view.findViewById(R.id.share_progress_text_view);

        this.homeHolder = (LinearLayout) view.findViewById(R.id.my_home_container);
        this.fragHolder = (FrameLayout) view.findViewById(R.id.my_frag_container);

        srl = (MSwipeRefreshLayout)view.findViewById(R.id.my_swipe_refresh);
        srl.setOnRefreshListener(this);

//        this.homeHolder.setVisibility(View.INVISIBLE);
//        this.fragHolder.setVisibility(View.INVISIBLE);
//        this.srl.setVisibility(View.INVISIBLE);

        srl.setOnChildScrollUpListener(new MSwipeRefreshLayout.OnChildScrollUpListener()
        {
            @Override
            public boolean canChildScrollUp()
            {
                List<Fragment> frags = myContext.getSupportFragmentManager().getFragments();

                if(frags.size() < 1) return false;

                RecyclerView rv = null;

                for(int i = 0; i < frags.size(); i++)
                {
                    if (frags.get(i) instanceof FlagsListFragment)
                    {
                        rv = ((FlagsListFragment) frags.get(i)).getRV();
                        break;
                    }
                }

                if(rv == null) return false;

                RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();

                int position = ((LinearLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition();

                // Log.d(TAG, "First completely visible item position: " + position);

                return position != 0 && rv.getAdapter().getItemCount() != 0;
            }
        });

//        AlphaAnimation inAnim = new AlphaAnimation(0, 1);
//        inAnim.setDuration(Utils.ANIMATION_DURATION);
//        progressBarHolder.setAnimation(inAnim);
//        progressBarHolder.setVisibility(View.VISIBLE);

        Fragment fragment = new FlagsListFragment();
        myContext.getSupportFragmentManager().beginTransaction().replace(R.id.my_swipe_refresh, fragment).commit();

        SupportMapFragment mapFragment = new SupportMapFragment();
        myContext.getSupportFragmentManager().beginTransaction().replace(R.id.my_map_holder, mapFragment).commit();
        mapFragment.getMapAsync(MainFragment.this);

        return view;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(this.receiver);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        this.gMap = googleMap;

        this.gMap.getUiSettings().setScrollGesturesEnabled(false);
        this.gMap.getUiSettings().setZoomGesturesEnabled(false);
        this.gMap.setMyLocationEnabled(true);

        this.updateMarkersOnMap();
    }

    private void updateMarkersOnMap() {

        List<Flag> flags= PlacesApplication.getInstance().getFlags();

        if(flags != null && this.gMap != null)
        {
            this.gMap.clear();

            //zooms around all the Flags
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (ParseObject p : flags)
            {
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
                                (int)(marker.getWidth() * 0.25f),
                                (int)(marker.getHeight() * 0.25f),
                                false);

                this.gMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(text)
                        .icon(BitmapDescriptorFactory.fromBitmap(halfSizeMarker))
                                // .icon(BitmapDescriptorFactory.fromResource(getIconForCategory(f.getCategory())))
                                //.icon(BitmapDescriptorFactory.defaultMarker(getCategoryColor(f.getCategory())))
                        .alpha(0.85f));
            }

            if(flags.size() > 0)
            {
                LatLngBounds bounds = builder.build();
//                this.gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Utils.MAP_BOUNDS));
                this.gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Utils.MAP_BOUNDS));
            }else{
                Location currentLocation = gMap.getMyLocation();
                if(currentLocation != null){
                    LatLng currentLocationLatLng = new LatLng(currentLocation.getLatitude(),
                            currentLocation.getLongitude());
                    this.gMap.animateCamera(CameraUpdateFactory
                            .newLatLngZoom(currentLocationLatLng, Utils.ZOOM_LVL));
                }
            }

        }
    }

    @Override
    public void onRefresh()
    {
        ((MainActivity)getActivity()).refresh();
        srl.setRefreshing(false);
    }
}
