package com.gcw.sapienza.places;

import android.content.Intent;
import android.content.SharedPreferences;
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

import com.gcw.sapienza.places.adapters.FlagsArrayAdapter;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.utils.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MosaicFragment extends Fragment{

    private static final String TAG = "MosaicFragment";

    private static View view;
    private static ListView listView;

    private static TextView textHeader;

    private static FlagsArrayAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
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
                bundle.putByteArray("pic", ((Flag) parent.getItemAtPosition(position)).getPic());
                bundle.putInt("temp", ((Flag) parent.getItemAtPosition(position)).getTemp());

                intent.putExtras(bundle);

                startActivity(intent);
            }
        });

        loadDefaultSettings();

        if(PlacesApplication.getLocation() != null && adapter == null)
            PlacesApplication.mService.queryParsewithLocation(PlacesApplication.getLocation());

        return view;
    }

    private void loadDefaultSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int range = prefs.getInt("seekBar", 1) + 1;
        Utils.MAP_RADIUS = range / 10f;
        Log.d(TAG, "Updated map radius to " + Utils.MAP_RADIUS);
        int step = Utils.stepValues[prefs.getInt("maxFetch", 1)];
        Utils.MAX_PINS = step;
        Log.d(TAG, "Updated max pins to " + Utils.MAX_PINS);
        MosaicFragment.updateHeaderText();
    }

    public static void updateHeaderText(){
        textHeader.setText("within " + (int)(Utils.MAP_RADIUS * 1000) + " meters");
    }

    public static void configureListViewWithFlags()
    {
        if(listView != null)
        {
            adapter =
                    new FlagsArrayAdapter
                                    (Utils.mainActivity,
                                    R.layout.flags_list_item,
                                    PlacesApplication.getPins(),
                                    Utils.mainActivity);

            listView.setAdapter(adapter);
        }
    }
}
