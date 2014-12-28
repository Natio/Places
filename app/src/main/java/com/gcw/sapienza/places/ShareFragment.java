package com.gcw.sapienza.places;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cgw.sapienza.places.model.Flag;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.SaveCallback;


public class ShareFragment extends Fragment{

    private static final String TAG = "ShareActivity";

    private TextView textView;

    private static View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.activity_share, container, false);

        this.textView = (TextView)view.findViewById(R.id.share_text_field);

        ((Button)view.findViewById(R.id.share_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShareFragment.this.share();
            }
        });

        return view;
    }

    private void share(){
        Location current_location = ((MainActivity)getActivity()).getLocation();

        if(current_location == null)
        {
            Log.d(TAG, "No GPS data");
            return;
        }

        Flag f = new Flag();
        ParseGeoPoint p = new ParseGeoPoint(current_location.getLatitude(), current_location.getLongitude());
        f.put("location",p);
        f.put("fbId", MainActivity.fbId);
        f.put("text",this.textView.getText().toString());
        f.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e != null){
                    Log.d(TAG, e.getMessage());
                }
                else
                {
                    resetShareFragment();
                }
            }
        });
    }

    public void resetShareFragment()
    {
        this.textView.setText("");

        view.findViewById(R.id.dummy).requestFocus();

        hideKeyboard();
    }

    public void hideKeyboard()
    {
        if(getActivity().getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(getActivity().getApplicationContext().INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }
}
