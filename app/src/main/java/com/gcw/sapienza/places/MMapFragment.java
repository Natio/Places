package com.gcw.sapienza.places;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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


public class MMapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MMapFragment";

    private GoogleMap gMap;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(LocationService.FOUND_NEW_FLAGS_NOTIFICATION)){
                MMapFragment.this.updateMarkersOnMap();
            }
        }
    };

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.d(TAG, "Visibility changed");
        updateMarkersOnMap();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_flags_map, container, false);

        SupportMapFragment mapFragment = new SupportMapFragment();
        this.getFragmentManager().beginTransaction().replace(R.id.map_holder, mapFragment).commit();
        mapFragment.getMapAsync(this);

        return view;
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
        this.gMap = null;
        LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(this.receiver);
    }


    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        this.gMap = googleMap;

        this.gMap.getUiSettings().setScrollGesturesEnabled(false);
        this.gMap.getUiSettings().setZoomGesturesEnabled(false);
        this.gMap.setMyLocationEnabled(true);

        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(this.receiver, new IntentFilter(LocationService.FOUND_NEW_FLAGS_NOTIFICATION));

        this.updateMarkersOnMap();
    }

    public void updateMarkersOnMap()
    {
        List<Flag> pins= PlacesApplication.getPins();

        if(pins != null && this.gMap != null)
        {
            this.gMap.clear();

            //zooms around all the Flags
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (ParseObject p : pins)
            {
                Flag f = (Flag) p;
                ParseGeoPoint location = f.getLocation();
                String text = f.getText();
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                builder.include(latLng);

                this.gMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(text)
                                .icon(BitmapDescriptorFactory.defaultMarker(getCategoryColor(f.getCategory())))
                                .alpha(0.8f));
            }

            if(pins.size() > 0){
                LatLngBounds bounds = builder.build();
                this.gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 70));
            }

        }
    }

    protected static float getCategoryColor(String category)
    {
        if (Utils.categories == null || category == null || category.equals(Utils.categories[0]) || category.isEmpty())
            return BitmapDescriptorFactory.HUE_RED;
        if (category.equals(Utils.categories[1])) return BitmapDescriptorFactory.HUE_AZURE;
        else if (category.equals(Utils.categories[2])) return BitmapDescriptorFactory.HUE_ORANGE;
        else if (category.equals(Utils.categories[3])) return BitmapDescriptorFactory.HUE_BLUE;
        else return BitmapDescriptorFactory.HUE_MAGENTA; // 'Food' category
    }
}
