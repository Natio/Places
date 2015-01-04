package com.gcw.sapienza.places;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.adapters.FlagsArrayAdapter;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class MosaicFragment extends Fragment{

    private static final String TAG = "MosaicFragment";

    private static View view;
    private static ListView listView;

    private static TextView textHeader;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.flags_list_layout, container, false);

        //retrieve the listviews
        listView = (ListView)view.findViewById(R.id.flags_list_view);

        View header = inflater.inflate(R.layout.header_flags_list, null);
        textHeader = (TextView)header.findViewById(R.id.header);
        textHeader.setText("within " + (int)(Utils.MAP_RADIUS * 1000) + " meters");
        listView.addHeaderView(header);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if(position == 0) {
                    startActivity(new Intent(getActivity().getApplicationContext(), SettingsActivity.class));
                    return;
                }

                Intent intent = new Intent(getActivity().getApplicationContext(), FlagActivity.class);

                Date date = ((Flag) parent.getItemAtPosition(position)).getDate();
                DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                String sDate = df.format(date);

                Bundle bundle = new Bundle();

                bundle.putString("text", ((Flag) parent.getItemAtPosition(position)).getText());
                bundle.putString("id", ((Flag) parent.getItemAtPosition(position)).getFbId());
                bundle.putString("date", sDate);

                intent.putExtras(bundle);

                startActivity(intent);
            }
        });

        fetchDefaultRadius();
        updateFlags();

        return view;
    }

    private void fetchDefaultRadius() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int range = prefs.getInt("seekBar", 2);
        Utils.MAP_RADIUS = range / 10f;
        Log.d(TAG, "Updated map radius to " + Utils.MAP_RADIUS);
        MosaicFragment.updateHeaderText();
    }

    public static void updateHeaderText(){
        textHeader.setText("within " + (int)(Utils.MAP_RADIUS * 1000) + " meters");
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
            FlagsArrayAdapter adapter = new FlagsArrayAdapter(this.getActivity(), R.layout.flags_list_item, flags, getActivity());

            listView.setAdapter(adapter);
        }
    }
}
