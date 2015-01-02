package com.gcw.sapienza.places.legacy;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.gcw.sapienza.places.MainActivity;
import com.gcw.sapienza.places.PlacesApplication;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.SaveCallback;

import com.gcw.sapienza.places.model.Flag;

/**
 * Created by paolo on 12/12/14.
 */
public class ShareActivity extends Activity {
    private static final String TAG = "ShareActivity";

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_share);

        this.textView = (TextView)this.findViewById(R.id.share_text_field);

        ((Button)this.findViewById(R.id.share_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShareActivity.this.share();
            }
        });
    }

    private void share(){

        PlacesApplication app = (PlacesApplication)this.getApplication();
        Location current_location = app.getCurrentLocation();
        if(current_location == null){
            Log.d(TAG, "No GPS data");
            return;
        }
        Flag f = new Flag();
        ParseGeoPoint p = new ParseGeoPoint(current_location.getLatitude(), current_location.getLongitude());
        f.put("location",p);
        f.put("fbId", Utils.fbId);
        f.put("text",this.textView.getText().toString());
        f.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e != null){
                    Log.d(TAG, e.getMessage());
                }
                else{
                    ShareActivity.this.finish();
                }
            }
        });
    }
}
