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
import android.widget.Toast;

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

        if(location!=null)
        {
            initMap(googleMap);

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
                        initMap(googleMap);

                        ((MainActivity)getActivity()).refresh();
                    }
                }
            });
        }
    }

    protected void initMap(final GoogleMap googleMap)
    {
        LatLng lat_lng = new LatLng(location.getLatitude(), location.getLongitude());

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lat_lng, MAP_ZOOM));
        googleMap.getUiSettings().setScrollGesturesEnabled(false);

        gMap = googleMap;

        //show continuously my location on map
        gMap.setMyLocationEnabled(true);
    }

    public static void updateMarkersOnMap()
    {
        List<ParseObject> pins= PlacesApplication.getPins();

        if(pins != null && gMap != null)
        {
            gMap.clear();

            location = PlacesApplication.getLocation();
            LatLng lat_lng = new LatLng(location.getLatitude(), location.getLongitude());
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lat_lng, MAP_ZOOM));

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

            // add pin for your current location
//            gMap.addMarker(new MarkerOptions()
//                    .position(new LatLng(location.getLatitude(), location.getLongitude()))
//                    .title("You are here!")
//                    .alpha(1)).showInfoWindow();
        }
    }

    protected static float getCategoryColor(String category)
    {
        if(category==null || category.equals("Misc") || category.equals("")) return BitmapDescriptorFactory.HUE_CYAN;
        if(category.equals("Entertainment")) return BitmapDescriptorFactory.HUE_AZURE;
        else if(category.equals("Food")) return BitmapDescriptorFactory.HUE_BLUE;
        else if(category.equals("History")) return BitmapDescriptorFactory.HUE_GREEN;
        else if(category.equals("Culture")) return BitmapDescriptorFactory.HUE_MAGENTA;
        else if(category.equals("Landscapes")) return BitmapDescriptorFactory.HUE_VIOLET;
        else return BitmapDescriptorFactory.HUE_YELLOW; // 'State Of Mind' category
    }
}
