package com.gcw.sapienza.places;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import com.parse.*;

import java.util.ArrayList;
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
        ParseQuery<ParseObject> q = ParseQuery.getQuery("Posts");
        q.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> flags, ParseException e) {
                if (e == null) {
                    //if flags are retrieved configure the ListView

                    ArrayList<String> text_list = new ArrayList<>();
                    for(ParseObject p : flags){
                        text_list.add((String) p.get("text"));
                    }
                    FlagsListActivity.this.configureListViewWithFlags(text_list);
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
    private void configureListViewWithFlags(List<String> flags){
        Log.d(TAG, flags.toString());

        //retrieve the listview
        ListView listView = (ListView)this.findViewById(R.id.flags_list_view);
        //configure the adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.flags_list_item, R.id.firstLine, flags);

        listView.setAdapter(adapter);
    }

}
