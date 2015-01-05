package com.gcw.sapienza.places;

import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
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
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;


public class ShareFragment extends Fragment{

    private static final String TAG = "ShareFragment";

    private static View view;

    private static Spinner spinner;
    private static TextView textView;
    private static Button shareButton;
    protected static Button picButton;

    protected static Bitmap pic;
    protected static MediaStore.Video video;
    protected static MediaStore.Audio audio;

    protected static boolean isPicTaken = false;
    protected static boolean isVideoTaken = false;
    protected static boolean isSoundCaptured = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.activity_share, container, false);

        textView = (TextView)view.findViewById(R.id.share_text_field);
        textView.setGravity(Gravity.CENTER);

        shareButton = (Button)view.findViewById(R.id.share_button);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShareFragment.this.share();
                v.setClickable(false);
            }
        });

        picButton = (Button)view.findViewById(R.id.pic_button);

        spinner = (Spinner) view.findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.categories, R.layout.custom_spinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        return view;
    }

    private void share()
    {
        Location current_location =  PlacesApplication.getLocation();
        if(PlacesApplication.isRunningOnEmulator){
            current_location = LocationService.getRandomLocation(current_location, 100);
            Log.d(TAG, "Generata Posizione casuale per simulatore: "+current_location);
        }

        if(current_location == null)
        {
            Toast.makeText(getActivity().getApplicationContext(), "Please enable GPS/Network service", Toast.LENGTH_LONG).show();
            Log.d(TAG, "No GPS data");

            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("reason", "Share with No GPS");
            ParseAnalytics.trackEventInBackground("sharing_failed", dimensions);
            resetShareFragment();
            return;
        }

        final Flag f = new Flag();
        ParseGeoPoint p = new ParseGeoPoint(current_location.getLatitude(), current_location.getLongitude());

        if(Utils.fbId.equals(""))
        {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Couldn't retrieve your Facebook credentials,\nplease check your internet connection.",
                    Toast.LENGTH_LONG).show();
            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("reason", "Share with No Facebook");
            ParseAnalytics.trackEventInBackground("sharing_failed", dimensions);
            resetShareFragment();
            return;
        }
        final String category = spinner.getSelectedItem().toString();
        f.put("fbId", Utils.fbId);
        f.put("category", category);
        f.put("location",p);
        f.put("text",this.textView.getText().toString());
        f.put("weather", PlacesApplication.weather);


        new Thread(new Runnable()
        {
            @Override
            public void run() {

                if (isPicTaken)
                {
                    if (pic == null)
                    {
                        Toast.makeText(getActivity(), "Error encountered while retrieving picture\nFlag won't be stored",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    else
                    {
                        Log.v(TAG, "Successfully retrieved pic.");

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        pic.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        byte[] byteArray = stream.toByteArray();
                        f.put("pic", byteArray);
                    }
                }

                f.saveInBackground(new SaveCallback()
                {
                    @Override
                    public void done(ParseException e)
                    {
                        if (e != null) {
                            Log.d(TAG, e.getMessage());
                            Map<String, String> dimensions = new HashMap<>();
                            dimensions.put("reason", e.getMessage());
                            ParseAnalytics.trackEventInBackground("sharing_failed", dimensions);
                        } else {
                            ((com.gcw.sapienza.places.MainActivity) getActivity()).refresh();
                            Map<String, String> dimensions = new HashMap<>();
                            dimensions.put("category", category);
                            ParseAnalytics.trackEventInBackground("sharing_succeded", dimensions);
                        }

                        resetShareFragment();
                        resetMedia();
                    }
                });
            }
        }).start();
    }

    protected void resetMedia()
    {
        isPicTaken = false;
        isVideoTaken = false;
        isSoundCaptured = false;

        pic = null;
        video = null;
        audio = null;
    }

    public void resetShareFragment()
    {
        this.textView.setText("");
        picButton.setText("Take a picture");

        hideKeyboard();

        Toast.makeText(getActivity().getApplicationContext(), "Flag has been placed!", Toast.LENGTH_LONG).show();
        shareButton.setClickable(true);
    }

    public void hideKeyboard()
    {
        if(getActivity().getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(getActivity().getApplicationContext().INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }
}
