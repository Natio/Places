package com.gcw.sapienza.places;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.cgw.sapienza.places.model.Flag;
import com.com.sapienza.places.adapters.FlagsArrayAdapter;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;

import java.util.List;


public class MosaicFragment extends Fragment{

    private static final String TAG = "MosaicFragment";

    private static View view;

    protected static final int MAP_RADIUS = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //Create the query and execute it in background
        ParseQuery<Flag> q = ParseQuery.getQuery(Flag.class);

        Location location = ((MainActivity)getActivity()).getLocation();

        if(location!=null)
        {
            ParseGeoPoint p = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
            q.whereWithinKilometers("location", p, MAP_RADIUS);
            q.findInBackground(new FindCallback<Flag>() {
                public void done(List<Flag> flags, ParseException e) {
                    if (e == null) {
                        //if flags are retrieved configure the ListView
                        MosaicFragment.this.configureListViewWithFlags(flags);
                    } else {
                        Log.d(TAG, "Error: " + e.getMessage());
                    }
                }
            });
        }

        view = inflater.inflate(R.layout.flags_list_layout, container, false);

        return view;
    }

    /**
     * Configures the listview for showing the flags passed as arguments
     * @param flags List of flags
     */
    private void configureListViewWithFlags(List<Flag> flags){
        Log.d(TAG, flags.toString());

        //retrieve the listview
        ListView listView = (ListView)this.getView().findViewById(R.id.flags_list_view);
        //configure the adapter
        FlagsArrayAdapter adapter = new FlagsArrayAdapter(this.getActivity(), R.layout.flags_list_item, flags);
        listView.setAdapter(adapter);
    }
}
