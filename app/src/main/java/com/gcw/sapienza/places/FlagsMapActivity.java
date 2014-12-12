package com.gcw.sapienza.places;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.cgw.sapienza.places.model.Flag;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;

import java.util.List;

/**
 * Activity for showing the flags on the map
 */
public class FlagsMapActivity extends Activity implements OnMapReadyCallback{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flags_map);
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.flags_map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {

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
        });
    }
}
