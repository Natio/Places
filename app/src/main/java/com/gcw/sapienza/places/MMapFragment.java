package com.gcw.sapienza.places;

import android.location.Location;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.utils.Utils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.util.List;


public class MMapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MMapFragment";
    private SupportMapFragment mapFragment;

    private static View view;

    protected static Location location;

    protected static final int MAP_ZOOM = 22;

    protected static GoogleMap gMap;

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.d(TAG, "Visibility changed");
        updateMarkersOnMap();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }

        try
        {
            view = inflater.inflate(R.layout.activity_flags_map, container, false);
        }
        catch(InflateException e)
        {
            Log.v(TAG, "Map not created, since it's already there.");
        }

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.flags_map);

        mapFragment.getMapAsync(this);

        return view;
    }

    @Override
    public void onMapReady(final GoogleMap googleMap)
    {
        location = PlacesApplication.getLocation();

        googleMap.getUiSettings().setScrollGesturesEnabled(false);
        gMap = googleMap;

        if(location!=null)
        {
            //show continuously my location on map
            gMap.setMyLocationEnabled(true);

            updateMarkersOnMap();
        }
        else
        {
            final Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    location = PlacesApplication.getLocation();

                    if (location == null) handler.postDelayed(this, Utils.UPDATE_DELAY);
                    else
                    {
                        gMap.setMyLocationEnabled(true);

                        if(getActivity() != null)
                            ((MainActivity)getActivity()).refresh();
                    }
                }
            });
        }
    }

    public static void updateMarkersOnMap()
    {
        List<Flag> pins= PlacesApplication.getPins();

        if(pins != null && gMap != null)
        {
            gMap.clear();

            location = PlacesApplication.getLocation();
            LatLng lat_lng = new LatLng(location.getLatitude(), location.getLongitude());
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lat_lng, MAP_ZOOM));

            for (ParseObject p : pins)
            {
                Flag f = (Flag) p;
                ParseGeoPoint location = f.getLocation();
                String text = f.getText();

                gMap.addMarker(new MarkerOptions()
                                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                                .title(text)
                                .icon(BitmapDescriptorFactory.defaultMarker(getCategoryColor(f.getCategory())))
                                .alpha(0.8f));
            }
        }
    }

    protected static float getCategoryColor(String category)
    {
        if (Utils.categories == null || category == null || category.equals(Utils.categories[0]) || category.equals(""))
            return BitmapDescriptorFactory.HUE_RED;
        if (category.equals(Utils.categories[1])) return BitmapDescriptorFactory.HUE_AZURE;
        else if (category.equals(Utils.categories[2])) return BitmapDescriptorFactory.HUE_ORANGE;
        else if (category.equals(Utils.categories[3])) return BitmapDescriptorFactory.HUE_BLUE;
        else return BitmapDescriptorFactory.HUE_MAGENTA; // 'Food' category
    }
}
