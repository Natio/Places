package com.gcw.sapienza.places;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
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

import com.gcw.sapienza.places.adapters.MSpinnerAdapter;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class ShareFragment extends Fragment{

    private static final String TAG = "ShareFragment";

    private static View mView;

    private Spinner spinner;
    private TextView textView;
    private Button shareButton;
    private Button picButton;

    private boolean isPicTaken = false;
    private boolean isVideoTaken = false;
    private boolean isSoundCaptured = false;

    protected static Bitmap pic;
    protected static MediaStore.Video video;
    protected static MediaStore.Audio audio;

    private final String FLAG_PLACED_TEXT = "Flag has been placed!";
    private final String ERROR_ENCOUNTERED_TEXT = "Error encountered while placing flag\nPlease try again";
    private final String FB_ID_NOT_FOUND_TEXT = "Couldn't retrieve your Facebook credentials\nPlease check your internet connection.";
    private final String EMPTY_FLAG_TEXT = "Please insert text or take a picture";
    private final String ENABLE_NETWORK_SERVICE_TEXT = "Please enable GPS/Network service";
    private final String PIC_NOT_FOUND_TEXT = "Error encountered while retrieving picture\nFlag won't be stored";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        this.mView = inflater.inflate(R.layout.activity_share, container, false);

        this.textView = (TextView)mView.findViewById(R.id.share_text_field);
        this.textView.setGravity(Gravity.CENTER);

        this.shareButton = (Button)this.mView.findViewById(R.id.share_button);
        this.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setClickable(false);
                ShareFragment.this.share();

            }
        });

        this.spinner = (Spinner)this.mView.findViewById(R.id.spinner);

        // ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.categories, R.layout.custom_spinner);
        // adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        MSpinnerAdapter adapter = new MSpinnerAdapter(getActivity().getApplicationContext(), Arrays.asList(getResources().getStringArray(R.array.categories)));

        this.spinner.setAdapter(adapter);

        return this.mView;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        Log.v(TAG, "onDestroy called.");
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        Log.v(TAG, "onDestroyView called.");
    }

    private void share()
    {
        Location current_location =  PlacesApplication.getLocation();
        if(PlacesApplication.isRunningOnEmulator){
            current_location = LocationService.getRandomLocation(current_location, 100);
            Log.d(TAG, "Generata Posizione casuale per simulatore: "+current_location);
        }

        //if there is no content
        //TODO remember to fix this when adding videos
        if(this.textView.getText().toString().length() == 0 && !isPicTaken)
        {
            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("reason", "Share without any content");
            ParseAnalytics.trackEventInBackground("sharing_failed", dimensions);

            resetShareFragment(EMPTY_FLAG_TEXT);
            return;
        }

        if(current_location == null)
        {
            Log.d(TAG, "No GPS data");

            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("reason", "Share with No GPS");
            ParseAnalytics.trackEventInBackground("sharing_failed", dimensions);
            resetShareFragment(ENABLE_NETWORK_SERVICE_TEXT);
            return;
        }

        final Flag f = new Flag();
        ParseGeoPoint p = new ParseGeoPoint(current_location.getLatitude(), current_location.getLongitude());

        if(Utils.fbId.equals(""))
        {
            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("reason", "Share with No Facebook");
            ParseAnalytics.trackEventInBackground("sharing_failed", dimensions);
            resetShareFragment(FB_ID_NOT_FOUND_TEXT);
            return;
        }
        final String category = spinner.getSelectedItem().toString();
        f.put("fbId", Utils.fbId);
        f.put("category", category);
        f.put("location",p);
        f.put("text",this.textView.getText().toString());
        f.put("weather", PlacesApplication.getWeather());


        new Thread(new Runnable()
        {
            @Override
            public void run() {

                if (ShareFragment.this.isPicTaken)
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

                            resetShareFragment(ERROR_ENCOUNTERED_TEXT);
                        }
                        else
                        {
                            ((com.gcw.sapienza.places.MainActivity) getActivity()).refresh();
                            Map<String, String> dimensions = new HashMap<>();
                            dimensions.put("category", category);
                            ParseAnalytics.trackEventInBackground("sharing_succeded", dimensions);

                            resetShareFragment(FLAG_PLACED_TEXT);
                        }
                        resetMedia();
                    }
                });
            }
        }).start();
    }

    @Deprecated
    protected void setPicButtonAsPicTaken()
    {
        this.mView = getView();
        this.picButton = (Button)this.mView.findViewById(R.id.pic_button);
        this.picButton.setText("Picture taken ✓");
    }

    @Override
    public void onResume() {
        super.onResume();

        onVisiblePage();
    }

    public void onVisiblePage()
    {
        Log.v(TAG, "ShareFragment visible!");

        this.mView = getView();
        if(mView != null)
        {
            this.picButton = (Button) this.mView.findViewById(R.id.pic_button);

            if (pic != null) {
                this.isPicTaken = true;
                this.picButton.setText("Picture taken ✓");
            } else {
                this.isPicTaken = false;
                this.picButton.setText("Attach picture");
            }
        }
    }

    protected void resetMedia()
    {
        this.isPicTaken = false;
        this.isVideoTaken = false;
        this.isSoundCaptured = false;

        pic = null;
        video = null;
        audio = null;
    }

    public void resetShareFragment(String toastText)
    {
        this.mView = getView();
        this.textView.setText("");
        this.picButton = (Button)this.mView.findViewById(R.id.pic_button);
        this.picButton.setText("Attach picture");

        hideKeyboard();

        Toast.makeText(getActivity().getApplicationContext(), toastText, Toast.LENGTH_LONG).show();
        this.shareButton.setClickable(true);

    }

    public void hideKeyboard()
    {
        if(getActivity().getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }

    //=========================================
    // Getters & Setters
    //=========================================

    public Button getPicButton() {
        return picButton;
    }

    public boolean isPicTaken() {
        return isPicTaken;
    }

    public void setPicTaken(boolean isPicTaken) {
        this.isPicTaken = isPicTaken;
    }

    public boolean isVideoTaken() {
        return isVideoTaken;
    }

    public void setVideoTaken(boolean isVideoTaken) {
        this.isVideoTaken = isVideoTaken;
    }

    public boolean isSoundCaptured() {
        return isSoundCaptured;
    }

    public void setSoundCaptured(boolean isSoundCaptured) {
        this.isSoundCaptured = isSoundCaptured;
    }

}
