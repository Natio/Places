package com.gcw.sapienza.places.legacy;

import android.app.Activity;
import android.os.Bundle;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;


/**
 * Activity for showing the flags on the map
 */
@Deprecated
public class FlagsMapActivity extends Activity implements OnMapReadyCallback{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flags_map);
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.flags_map);
        mapFragment.getMapAsync(this);*/
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
/*
        ParseQuery<Flag> q = ParseQuery.getQuery(Flag.class);
        ParseGeoPoint p = new ParseGeoPoint(41.8883656,12.5066291);
        q.whereWithinKilometers("location",p, 1);

        q.findInBackground(new FindCallback<Flag>() {
            public void done(List<Flag> flags, ParseException e) {
                if (e == null) {
                    for (Flag f : flags){
                        ParseGeoPoint location = f.getLocation();
                        String text = f.getText();

                        googleMap.addMarker(new MarkerOptions()
                                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                                .title(text));
                    }

                } else {
                    Log.d("score", "Error: " + e.getMessage());
                }
            }
        });*/
    }
}
