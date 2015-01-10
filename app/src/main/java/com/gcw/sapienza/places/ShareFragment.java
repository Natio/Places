package com.gcw.sapienza.places;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gcw.sapienza.places.adapters.MSpinnerAdapter;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.services.LocationService;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.gcw.sapienza.places.utils.Utils;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class ShareFragment extends Fragment{

    private static final String TAG = "ShareFragment";

    private static View mView;

    private Spinner spinner;
    private TextView textView;
    private Button shareButton;
    private FrameLayout progressBarHolder;

    protected static ImageView picButton;
    protected static ImageView micButton;
    protected static ImageView vidButton;

    protected static boolean clearMedia = true;

    protected static boolean isPicTaken = false;
    protected static boolean isVideoShoot = false;
    protected static boolean isSoundCaptured = false;

    protected static Bitmap pic; // for compatibiity
    protected static byte[] picture;
    protected static byte[] video;
    protected static byte[] audio;

    private static final int ANIMATION_DURATION = 300;

    private static final String FLAG_PLACED_TEXT = "Flag has been placed!";
    private static final String ERROR_ENCOUNTERED_TEXT = "Error encountered while placing flag\nPlease try again";
    private static final String FB_ID_NOT_FOUND_TEXT = "Couldn't retrieve your Facebook credentials\nPlease check your internet connection.";
    private static final String EMPTY_FLAG_TEXT = "Please insert text or take a picture";
    private static final String ENABLE_NETWORK_SERVICE_TEXT = "Please enable GPS/Network service";
    private static final String PIC_NOT_FOUND_TEXT = "Error encountered while retrieving picture\nFlag won't be stored";
    private static final String AUDIO_NOT_FOUND_TEXT = "Error encountered while retrieving recording\nFlag won't be stored";
    private static final String VIDEO_NOT_FOUND_TEXT = "Error encountered while retrieving video\nFlag won't be stored";

    protected static final float MEDIA_AVAILABLE_ALPHA = 0.3f;

    protected static final int CHUNK_SIZE = 4096;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView");

        this.mView = inflater.inflate(R.layout.activity_share, container, false);

        this.progressBarHolder = (FrameLayout)mView.findViewById(R.id.frame_layout);

        this.picButton = (ImageView)mView.findViewById(R.id.pic_button);
        this.micButton = (ImageView)mView.findViewById(R.id.mic_button);
        this.vidButton = (ImageView)mView.findViewById(R.id.vid_button);

        this.picButton.setOnLongClickListener((View.OnLongClickListener)getActivity());
        this.micButton.setOnLongClickListener((View.OnLongClickListener)getActivity());
        this.vidButton.setOnLongClickListener((View.OnLongClickListener)getActivity());

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

        if(!FacebookUtils.getInstance().hasCurrentUserId())
        {
            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("reason", "Share with No Facebook");
            ParseAnalytics.trackEventInBackground("sharing_failed", dimensions);
            resetShareFragment(FB_ID_NOT_FOUND_TEXT);
            return;
        }

        Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(Utils.VIBRATION_DURATION);

        final String category = spinner.getSelectedItem().toString();

        f.put("fbId", FacebookUtils.getInstance().getCurrentUserId());
        f.put("category", category);
        f.put("location",p);
        f.put("text",this.textView.getText().toString());
        f.put("weather", PlacesApplication.getWeather());


        new Thread(new Runnable()
        {
            @Override
            public void run() {

                if (ShareFragment.isPicTaken)
                {
                    if (pic == null)
                    {
                        Toast.makeText(getActivity(), PIC_NOT_FOUND_TEXT,
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

                if (ShareFragment.isSoundCaptured)
                {
                    if (audio == null)
                    {
                        Toast.makeText(getActivity(), AUDIO_NOT_FOUND_TEXT,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    else
                    {
                        Log.v(TAG, "Successfully retrieved recording.");

                        ParseFile parse_audio = new ParseFile(System.currentTimeMillis()+".3gp", ShareFragment.audio);
                        parse_audio.saveInBackground();
                        f.put("audio", parse_audio);
                    }
                }

                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        AlphaAnimation inAnim = new AlphaAnimation(0, 1);
                        inAnim.setDuration(ANIMATION_DURATION);
                        progressBarHolder.setAnimation(inAnim);
                        progressBarHolder.setVisibility(View.VISIBLE);
                    }
                });

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

                        getActivity().runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                AlphaAnimation outAnim = new AlphaAnimation(1, 0);
                                outAnim.setDuration(ANIMATION_DURATION);
                                progressBarHolder.setAnimation(outAnim);
                                progressBarHolder.setVisibility(View.GONE);
                            }
                        });

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
        this.picButton = (ImageView)this.mView.findViewById(R.id.pic_button);
        this.picButton.setAlpha(MEDIA_AVAILABLE_ALPHA);
    }

    @Override
    public void onResume() {
        super.onResume();

        onVisiblePage();
    }

    public void onVisiblePage()
    {
        Log.v(TAG, "ShareFragment visible!");

        if(mView == null) mView = getView();
        if(mView != null)
        {
            picButton = (ImageView)mView.findViewById(R.id.pic_button);
            micButton = (ImageView)mView.findViewById(R.id.mic_button);

            if (pic != null) {
                isPicTaken = true;
                this.picButton.setAlpha(MEDIA_AVAILABLE_ALPHA);
            } else {
                isPicTaken = false;
                this.picButton.setAlpha(1f);
            }

            if (audio != null) {
                isSoundCaptured = true;
                this.micButton.setAlpha(MEDIA_AVAILABLE_ALPHA);
            } else {
                isSoundCaptured = false;
                this.micButton.setAlpha(1f);
            }
        }
    }

    protected void resetMedia()
    {
        this.isPicTaken = false;
        this.isVideoShoot = false;
        this.isSoundCaptured = false;

        pic = null;
        video = null;
        audio = null;

        Log.v(TAG, "Media has been cleared!");
    }

    public void resetShareFragment(String toastText)
    {
        this.mView = getView();

        this.textView.setText("");

        this.picButton = (ImageView)this.mView.findViewById(R.id.pic_button);
        this.picButton.setAlpha(1f);

        this.micButton = (ImageView)this.mView.findViewById(R.id.mic_button);
        this.micButton.setAlpha(1f);

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
}
