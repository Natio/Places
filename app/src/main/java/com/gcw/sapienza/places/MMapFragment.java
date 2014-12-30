package com.gcw.sapienza.places;

import android.location.Location;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gcw.sapienza.places.model.Flag;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;

import java.util.List;


public class MMapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MMapFragment";
    private SupportMapFragment mapFragment;
    private static View view;

    protected Location location;

    protected static final int MAP_RADIUS = 1;
    protected static final int MAP_ZOOM = 18;

    protected GoogleMap gMap;

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
    public void onMapReady(final GoogleMap googleMap) {
        location = ((MainActivity)getActivity()).getLocation();
        LatLng lat_lng = new LatLng(location.getLatitude(), location.getLongitude());

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lat_lng, MAP_ZOOM));

        gMap = googleMap;

        if(location!=null) updateMarkersOnMap();
    }

    protected void updateMarkersOnMap()
    {
        ParseQuery<Flag> q = ParseQuery.getQuery(Flag.class);

        gMap.clear();

        ParseGeoPoint p = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        q.whereWithinKilometers("location", p, MAP_RADIUS);

        q.findInBackground(new FindCallback<Flag>() {
            public void done(List<Flag> flags, ParseException e) {
                if (e == null) {
                    for (Flag f : flags) {
                        ParseGeoPoint location = f.getLocation();
                        String text = f.getText();

                        gMap.addMarker(new MarkerOptions()
                                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                                .title(text));
                    }
                } else {
                    Log.d(TAG, "Error: " + e.getMessage());
                }
            }
        });
    }
}
