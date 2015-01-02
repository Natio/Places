package com.gcw.sapienza.places;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.adapters.FlagsArrayAdapter;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;

import java.util.List;


public class MosaicFragment extends Fragment{

    private static final String TAG = "MosaicFragment";

    private static View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        updateFlags();

        view = inflater.inflate(R.layout.flags_list_layout, container, false);

        return view;
    }

    protected void updateFlags()
    {
        //Create the query and execute it in background
        ParseQuery<Flag> q = ParseQuery.getQuery(Flag.class);

        Location location = PlacesApplication.getLocation();

        if(location!=null)
        {
            ParseGeoPoint p = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
            q.whereWithinKilometers("location", p, Utils.MAP_RADIUS);
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
    }

    /**
     * Configures the listview for showing the flags passed as arguments
     * @param flags List of flags
     */
    private void configureListViewWithFlags(final List<Flag> flags)
    {
        if(this.getView() != null)
        {
            //retrieve the listviews
            ListView listView = (ListView)this.getView().findViewById(R.id.flags_list_view);
            //configure the adapter
            FlagsArrayAdapter adapter = new FlagsArrayAdapter(this.getActivity(), R.layout.flags_list_item, flags);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    Intent intent = new Intent(getActivity().getApplicationContext(), FlagActivity.class);

                    Bundle bundle = new Bundle();

                    bundle.putString("text", ((Flag) parent.getItemAtPosition(position)).getText());
                    bundle.putString("id", ((Flag) parent.getItemAtPosition(position)).getFbId());

                    intent.putExtras(bundle);

                    startActivity(intent);
                }
            });
        }
    }
}
