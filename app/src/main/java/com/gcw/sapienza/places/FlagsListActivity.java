package com.gcw.sapienza.places;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.cgw.sapienza.places.model.Flag;
import com.com.sapienza.places.adapters.FlagsArrayAdapter;
import com.parse.FindCallback;
import com.parse.ParseQuery;

import com.parse.*;

import java.util.List;






/**
 * Activity to show the list of flags
 */
public class FlagsListActivity extends Activity {
    private static final String TAG = "FlagsListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.flags_list_layout);

        //Create the query and execute it in background
        //TODO add location filtering
        ParseQuery<Flag> q = ParseQuery.getQuery(Flag.class);
        q.findInBackground(new FindCallback<Flag>() {
            public void done(List<Flag> flags, ParseException e) {
                if (e == null) {
                    //if flags are retrieved configure the ListView
                    FlagsListActivity.this.configureListViewWithFlags(flags);
                } else {
                    Log.d("score", "Error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Configures the listview for showing the flags passed as arguments
     * @param flags List of flags
     */
    private void configureListViewWithFlags(List<Flag> flags){
        Log.d(TAG, flags.toString());

        //retrieve the listview
        ListView listView = (ListView)this.findViewById(R.id.flags_list_view);
        //configure the adapter
        FlagsArrayAdapter adapter = new FlagsArrayAdapter(this, R.layout.flags_list_item, flags);
        listView.setAdapter(adapter);
    }

}