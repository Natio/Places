package com.gcw.sapienza.places;

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

    private  View mView;

    private Spinner spinner;
    private TextView textView;
    private Button shareButton;
    private Button picButton;

    private Bitmap pic;
    private MediaStore.Video video;
    private MediaStore.Audio audio;

    private boolean isPicTaken = false;
    private boolean isVideoTaken = false;
    private boolean isSoundCaptured = false;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        this.mView = inflater.inflate(R.layout.activity_share, container, false);

        this.textView = (TextView)mView.findViewById(R.id.share_text_field);
        this.textView.setGravity(Gravity.CENTER);

        this.shareButton = (Button)this.mView.findViewById(R.id.share_button);
        this.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShareFragment.this.share();
                v.setClickable(false);
            }
        });

        this.spinner = (Spinner)this.mView.findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.categories, R.layout.custom_spinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
        if(this.textView.getText().toString().length() == 0 && !isPicTaken){
            Toast.makeText(getActivity().getApplicationContext(), "Please Insert text or take a picture", Toast.LENGTH_LONG).show();
            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("reason", "Share without any content");
            ParseAnalytics.trackEventInBackground("sharing_failed", dimensions);

            resetShareFragment();
            return;
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

    protected void setPicButtonAsPicTaken()
    {
        this.mView = getView();
        this.picButton = (Button)this.mView.findViewById(R.id.pic_button);
        this.picButton.setText("Picture taken ✓");
    }

    protected void resetMedia()
    {
        this.isPicTaken = false;
        this.isVideoTaken = false;
        this.isSoundCaptured = false;

        this.pic = null;
        this.video = null;
        this.audio = null;
    }

    public void resetShareFragment()
    {
        this.textView.setText("");
        this.picButton.setText("Take a picture");

        hideKeyboard();

        Toast.makeText(getActivity().getApplicationContext(), "Flag has been placed!", Toast.LENGTH_LONG).show();
        this.shareButton.setClickable(true);
    }

    public void hideKeyboard()
    {
        if(getActivity().getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(getActivity().getApplicationContext().INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }

    //=========================================
    // Getters & Setters
    //=========================================

    public Button getPicButton() {
        return picButton;
    }

    public Bitmap getPic() {
        return pic;
    }

    public void setPic(Bitmap pic) {
        this.pic = pic;
    }

    public MediaStore.Video getVideo() {
        return video;
    }

    public void setVideo(MediaStore.Video video) {
        this.video = video;
    }

    public MediaStore.Audio getAudio() {
        return audio;
    }

    public void setAudio(MediaStore.Audio audio) {
        this.audio = audio;
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
