package com.gcw.sapienza.places;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class ShareFragment extends Fragment{

    private static final String TAG = "ShareActivity";

    private TextView textView;

    private static View view;

    private Spinner spinner;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.activity_share, container, false);

        textView = (TextView)view.findViewById(R.id.share_text_field);
        textView.setGravity(Gravity.CENTER);

        ((Button)view.findViewById(R.id.share_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShareFragment.this.share();
            }
        });

        spinner = (Spinner) view.findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.categories, R.layout.custom_spinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        return view;
    }

    private void share()
    {
        Location current_location = PlacesApplication.getLocation();

        if(current_location == null)
        {
            Toast.makeText(getActivity().getApplicationContext(), "Please enable GPS/Network service", Toast.LENGTH_LONG).show();
            Log.d(TAG, "No GPS data");
            return;
        }

        Flag f = new Flag();
        ParseGeoPoint p = new ParseGeoPoint(current_location.getLatitude(), current_location.getLongitude());
        f.put("category", spinner.getSelectedItem().toString());
        f.put("location",p);
        if(!Utils.fbId.equals("")) f.put("fbId", Utils.fbId);
        else f.put("user", ParseUser.getCurrentUser().getUsername());
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
                    ((com.gcw.sapienza.places.MainActivity)getActivity()).refresh();
                }
            }
        });
    }

    public void resetShareFragment()
    {
        this.textView.setText("");

        hideKeyboard();

        Toast.makeText(getActivity().getApplicationContext(), "Flag has been placed!", Toast.LENGTH_LONG).show();
    }

    public void hideKeyboard()
    {
        if(getActivity().getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(getActivity().getApplicationContext().INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }
}
