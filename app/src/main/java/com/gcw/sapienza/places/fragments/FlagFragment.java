package com.gcw.sapienza.places.fragments;


import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.ShareActivity;
import com.gcw.sapienza.places.model.CustomParseObject;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.utils.FacebookUtilCallback;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.squareup.picasso.Picasso;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mic_head on 02/02/15.
 */
public class FlagFragment extends Fragment implements View.OnClickListener, View.OnTouchListener {

    private String text;
    private String id;
    private String date;
    private String weather;
    private String category;
    private boolean inPlace;
    private String flagId;

    private int wowCount;
    private int lolCount;
    private int booCount;

    private MediaPlayer mediaPlayer;
    private VideoView vv;
    private ImageView iw;
    private TextView authorTextView;
    private RelativeLayout frameLayout;
    private ImageView playVideoButton;
    private FrameLayout videoHolder;
    private ImageView audioHolder;

    private Button wowButton;
    private Button lolButton;
    private Button booButton;

    public static enum MediaType{ PIC, AUDIO, VIDEO, NONE }
    private MediaType mediaType;
    private ParseFile mediaFile;

    protected String userId;

    private View view;

    private static final String TAG = "FlagFragment";

    private static final int WOW_CODE = 0;
    private static final int LOL_CODE = 1;
    private static final int BOO_CODE = 2;

    /**
     *  Must be called BEFORE adding the fragment to the
     * @param mediaFile the file to display
     * @param type the type of the file
     */
    public void setMedia(ParseFile mediaFile, MediaType type){
        this.mediaType = type;
        this.mediaFile = mediaFile;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        text = bundle.getString("text");
        id = bundle.getString("id");
        date = bundle.getString("date");
        weather = bundle.getString("weather");
        category = bundle.getString("category");
        inPlace = bundle.getBoolean("inPlace");
        flagId = bundle.getString("flagId");

        wowCount = bundle.getInt("wowCount");
        lolCount = bundle.getInt("lolCount");
        booCount = bundle.getInt("booCount");

        userId = FacebookUtils.getInstance().getCurrentUserId();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if(mediaPlayer != null) mediaPlayer.release();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);

        view = inflater.inflate(R.layout.flag_layout, container, false);

        iw = (ImageView)view.findViewById(R.id.pic);
        vv = (VideoView)view.findViewById(R.id.vid);
        EditText flagText = (EditText)view.findViewById(R.id.text);
        authorTextView = (TextView)view.findViewById(R.id.author);
        ImageView profilePicimageView = (ImageView)view.findViewById(R.id.profile_pic);
        frameLayout = (RelativeLayout)view.findViewById(R.id.frame_layout);
        playVideoButton = (ImageView)view.findViewById(R.id.play_video_button);
        videoHolder = (FrameLayout)view.findViewById(R.id.video_holder);
        audioHolder = (ImageView) view.findViewById(R.id.audio);

        wowButton = (Button)view.findViewById(R.id.wow_button);
        lolButton = (Button)view.findViewById(R.id.lol_button);
        booButton = (Button)view.findViewById(R.id.boo_button);

        iw.setOnClickListener(this);
        vv.setOnTouchListener(this);
        audioHolder.setOnClickListener(this);
        frameLayout.setOnClickListener(this);
        // playVideoButton.setOnClickListener(this);

        wowButton.setOnClickListener(this);
        lolButton.setOnClickListener(this);
        booButton.setOnClickListener(this);

        this.changeLayoutAccordingToMediaType();

        flagText.setText(text);

        final String weatherString = (weather == null || weather.isEmpty()) ? "" : "\nWeather: " + weather;
        final String bottomLineText = date + weatherString + "\nCategory: " + category;

        FacebookUtils.getInstance().getFacebookUsernameFromID(this.id, new FacebookUtilCallback() {
            @Override
            public void onResult(String result, Exception e) {
                authorTextView.setText("Author: " + result + bottomLineText);
            }
        });

        FacebookUtils.getInstance().loadProfilePicIntoImageView(this.id, profilePicimageView, FacebookUtils.PicSize.LARGE);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener()
        {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if (keyCode == KeyEvent.KEYCODE_BACK)
                {
                    if(frameLayout.getVisibility() == View.VISIBLE)
                    {
                        frameLayout.setVisibility(View.GONE);
                        return true;
                    }
                }
                return false;
            }
        });

        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        ParseQuery<Flag> queryPosts = ParseQuery.getQuery("Posts");
        queryPosts.whereEqualTo("objectId", flagId);

        queryPosts.findInBackground(new FindCallback<Flag>()
        {
            public void done(List<Flag> markers, ParseException e)
            {
                if (e == null && markers.size() != 0)
                {
                    Flag flag = markers.get(0);

                    wowCount = flag.getInt("wowCount");
                    lolCount = flag.getInt("lolCount");
                    booCount = flag.getInt("booCount");
                }
            }
        });

        wowButton.setText(wowButton.getText() + " (" + wowCount + ")");
        lolButton.setText(lolButton.getText() + " (" + lolCount + ")");
        booButton.setText(booButton.getText() + " (" + booCount + ")");

        ParseQuery<CustomParseObject> queryW = ParseQuery.getQuery("Wow_Lol_Boo");
        queryW.whereEqualTo("fbId", userId);
        queryW.whereEqualTo("flagId", flagId);
        queryW.whereEqualTo("boolWow", true);

        queryW.findInBackground(new FindCallback<CustomParseObject>()
        {
            public void done(List<CustomParseObject> markers, ParseException e)
            {
                if (e == null && markers.size() != 0)
                {
                    wowButton.setText("You wow this." + " (" + wowCount + ")");
                }
            }
        });

        ParseQuery<CustomParseObject> queryL = ParseQuery.getQuery("Wow_Lol_Boo");
        queryL.whereEqualTo("fbId", userId);
        queryL.whereEqualTo("flagId", flagId);
        queryL.whereEqualTo("boolLol", true);

        queryL.findInBackground(new FindCallback<CustomParseObject>()
        {
            public void done(List<CustomParseObject> markers, ParseException e)
            {
                if (e == null && markers.size() != 0)
                {
                    lolButton.setText("You lol this." + " (" + lolCount + ")");
                }
            }
        });

        ParseQuery<CustomParseObject> queryB = ParseQuery.getQuery("Wow_Lol_Boo");
        queryB.whereEqualTo("fbId", userId);
        queryB.whereEqualTo("flagId", flagId);
        queryB.whereEqualTo("boolBoo", true);

        queryB.findInBackground(new FindCallback<CustomParseObject>()
        {
            public void done(List<CustomParseObject> markers, ParseException e)
            {
                if (e == null && markers.size() != 0)
                {
                    booButton.setText("You boo this." + " (" + booCount + ")");
                }
            }
        });
    }

    @Override
    public void onClick(View v)
    {
        if(v.getId() == R.id.frame_layout) frameLayout.setVisibility(View.GONE);
        else if(v.getId() == R.id.pic) frameLayout.setVisibility(View.VISIBLE);
        // else if(v.getId() == playVideoButton.getId()) playVideo();
        else if(v.getId() == R.id.audio) playRecording();
        else if(v.getId() == R.id.wow_button) wlbFlag(WOW_CODE);
        else if(v.getId() == R.id.lol_button) wlbFlag(LOL_CODE);
        else if(v.getId() == R.id.boo_button) wlbFlag(BOO_CODE);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        if(v.getId() == vv.getId() && event.getAction() == MotionEvent.ACTION_DOWN){
            return playVideo();
        }

        return false;
    }

    private void playRecording()
    {
        if(mediaPlayer.isPlaying())
        {
            mediaPlayer.pause();
            audioHolder.setImageResource(R.drawable.play_button);
        }
        else
        {
            mediaPlayer.start();
            audioHolder.setImageResource(R.drawable.pause_button);
        }
    }

    private boolean playVideo()
    {
        if(vv.isPlaying())
        {
            vv.pause();
            playVideoButton.setVisibility(View.VISIBLE);
        }
        else
        {
            // vv.resume();
            vv.start();
            playVideoButton.setVisibility(View.GONE);
        }

        return true;
    }


    private void onVideoDownloaded(String videoPath){
        Uri videoUri = Uri.parse(videoPath);
        vv.setVideoURI(videoUri);
        playVideoButton.setVisibility(View.VISIBLE);
        // playVideo();

        vv.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playVideoButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void onAudioDownloaded(String audioPath){
        try {
            audioHolder.setVisibility(View.VISIBLE);
            File temp = new File(audioPath);

            mediaPlayer = new MediaPlayer();

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    audioHolder.setImageResource(R.drawable.play_button);
                }
            });

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            FileInputStream inStream = new FileInputStream(temp);
            mediaPlayer.setDataSource(inStream.getFD());

            mediaPlayer.prepare();
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }

    private void wlbFlag(int code)
    {
        ParseQuery<CustomParseObject> queryWLB = ParseQuery.getQuery("Wow_Lol_Boo");
        queryWLB.whereEqualTo("fbId", userId);
        queryWLB.whereEqualTo("flagId", flagId);

        switch(code)
        {
            case WOW_CODE:
                wowButton.setClickable(false);

                queryWLB.findInBackground(new FindCallback<CustomParseObject>() {
                    public void done(List<CustomParseObject> markers, ParseException e) {
                        if (e == null && markers.size() != 0) {
                            CustomParseObject obj = markers.get(0);

                            boolean boolWow = obj.getBoolean("boolWow");

                            if (!boolWow) {
                                obj.setWowBoolean(true);
                                obj.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        updateWLBCount(WOW_CODE, true);
                                    }
                                });
                            } else {
                                obj.setWowBoolean(false);
                                obj.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        updateWLBCount(WOW_CODE, false);
                                    }
                                });
                            }
                        } else if (markers.size() == 0) {
                            CustomParseObject obj = new CustomParseObject();
                            obj.setUser(ParseUser.getCurrentUser());
                            obj.setFlagId(flagId);
                            obj.setFacebookId(FacebookUtils.getInstance().getCurrentUserId());
                            obj.setWowBoolean(true);
                            obj.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    updateWLBCount(WOW_CODE, true);
                                }
                            });
                        } else {
                            Toast.makeText(getActivity(), "Error encounterd while accessing database", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Error encounterd while retrieving table entry on Parse.com");

                            wowButton.setClickable(true);
                        }
                    }
                });

                break;

            case LOL_CODE:
                lolButton.setClickable(false);

                queryWLB.findInBackground(new FindCallback<CustomParseObject>() {
                    public void done(List<CustomParseObject> markers, ParseException e) {
                        if (e == null && markers.size() != 0) {
                            CustomParseObject obj = markers.get(0);

                            boolean boolLol = obj.getBoolean("boolLol");

                            if (!boolLol) {
                                obj.setLolBoolean(true);
                                obj.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        updateWLBCount(LOL_CODE, true);
                                    }
                                });
                            } else {
                                obj.setLolBoolean(false);
                                obj.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        updateWLBCount(LOL_CODE, false);
                                    }
                                });
                            }
                        } else if (markers.size() == 0) {
                            CustomParseObject obj = new CustomParseObject();
                            obj.setUser(ParseUser.getCurrentUser());
                            obj.setFlagId(flagId);
                            obj.setFacebookId(FacebookUtils.getInstance().getCurrentUserId());
                            obj.setLolBoolean(true);
                            obj.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    updateWLBCount(LOL_CODE, true);
                                }
                            });
                        } else {
                            Toast.makeText(getActivity(), "Error encounterd while accessing database", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Error encounterd while retrieving table entry on Parse.com");

                            lolButton.setClickable(true);
                        }
                    }
                });

                break;

            case BOO_CODE:
                booButton.setClickable(false);

                queryWLB.findInBackground(new FindCallback<CustomParseObject>() {
                    public void done(List<CustomParseObject> markers, ParseException e) {
                        if (e == null && markers.size() != 0) {
                            CustomParseObject obj = markers.get(0);

                            boolean boolBoo = obj.getBoolean("boolBoo");

                            if (!boolBoo) {
                                obj.setBooBoolean(true);
                                obj.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        updateWLBCount(BOO_CODE, true);
                                    }
                                });
                            } else {
                                obj.setBooBoolean(false);
                                obj.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        updateWLBCount(BOO_CODE, false);
                                    }
                                });
                            }
                        } else if (markers.size() == 0) {
                            CustomParseObject obj = new CustomParseObject();
                            obj.setUser(ParseUser.getCurrentUser());
                            obj.setFlagId(flagId);
                            obj.setFacebookId(FacebookUtils.getInstance().getCurrentUserId());
                            obj.setBooBoolean(true);
                            obj.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    updateWLBCount(BOO_CODE, true);
                                }
                            });
                        } else {
                            Toast.makeText(getActivity(), "Error encounterd while accessing database", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Error encounterd while retrieving table entry on Parse.com");

                            booButton.setClickable(true);
                        }
                    }
                });

        }
    }

    private void updateWLBCount(int code, final boolean increment)
    {
        ParseQuery<Flag> queryPosts = ParseQuery.getQuery("Posts");
        queryPosts.whereEqualTo("objectId", flagId);

        switch(code)
        {
            case WOW_CODE:
                queryPosts.findInBackground(new FindCallback<Flag>()
                {
                    public void done(List<Flag> markers, ParseException e)
                    {
                        if (e == null && markers.size() != 0)
                        {
                            Flag flag = markers.get(0);

                            final int wowCount = flag.getInt("wowCount");

                            if(increment)
                            {
                                flag.increment("wowCount");

                                flag.saveInBackground(new SaveCallback()
                                {
                                    @Override
                                    public void done(ParseException e)
                                    {
                                        updateWowButtonText(true, wowCount + 1);

                                        wowButton.setClickable(true);
                                    }
                                });
                            }
                            else
                            {
                                flag.increment("wowCount", -1);

                                flag.saveInBackground(new SaveCallback()
                                {
                                    @Override
                                    public void done(ParseException e)
                                    {
                                        updateWowButtonText(false, wowCount - 1);

                                        wowButton.setClickable(true);
                                    }
                                });
                            }
                        }
                        else
                        {
                            Toast.makeText(getActivity(), "Error encounterd while accessing database", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Error encounterd while retrieving table entry on Parse.com");

                            wowButton.setClickable(true);
                        }
                    }
                });

                break;

            case LOL_CODE:
                queryPosts.findInBackground(new FindCallback<Flag>()
                {
                    public void done(List<Flag> markers, ParseException e)
                    {
                        if (e == null && markers.size() != 0)
                        {
                            Flag flag = markers.get(0);

                            final int lolCount = flag.getInt("lolCount");

                            if(increment)
                            {
                                flag.increment("lolCount");

                                flag.saveInBackground(new SaveCallback()
                                {
                                    @Override
                                    public void done(ParseException e)
                                    {
                                        updateLolButtonText(true, lolCount + 1);

                                        lolButton.setClickable(true);
                                    }
                                });
                            }
                            else
                            {
                                flag.increment("lolCount", -1);

                                flag.saveInBackground(new SaveCallback()
                                {
                                    @Override
                                    public void done(ParseException e)
                                    {
                                        updateLolButtonText(false, lolCount - 1);

                                        lolButton.setClickable(true);
                                    }
                                });
                            }
                        }
                        else
                        {
                            Toast.makeText(getActivity(), "Error encounterd while accessing database", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Error encounterd while retrieving table entry on Parse.com");

                            wowButton.setClickable(true);
                        }
                    }
                });

                break;

            case BOO_CODE:
                queryPosts.findInBackground(new FindCallback<Flag>()
                {
                    public void done(List<Flag> markers, ParseException e)
                    {
                        if (e == null && markers.size() != 0)
                        {
                            Flag flag = markers.get(0);

                            final int booCount = flag.getInt("booCount");

                            if(increment)
                            {
                                flag.increment("booCount");

                                flag.saveInBackground(new SaveCallback()
                                {
                                    @Override
                                    public void done(ParseException e)
                                    {
                                        updateBooButtonText(true, booCount + 1);

                                        booButton.setClickable(true);
                                    }
                                });
                            }
                            else
                            {
                                flag.increment("booCount", -1);

                                flag.saveInBackground(new SaveCallback()
                                {
                                    @Override
                                    public void done(ParseException e)
                                    {
                                        updateBooButtonText(false, booCount - 1);

                                        booButton.setClickable(true);
                                    }
                                });
                            }
                        }
                        else
                        {
                            Toast.makeText(getActivity(), "Error encounterd while accessing database", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Error encounterd while retrieving table entry on Parse.com");

                            booButton.setClickable(true);
                        }
                    }
                });
        }
    }

    private void updateWowButtonText(boolean wowed, int wowCount)
    {
        if(wowed) wowButton.setText("You wow this. (" + wowCount + ")");
        else wowButton.setText("WOW (" + wowCount + ")");
    }

    private void updateLolButtonText(boolean lold, int lolCount)
    {
        if(lold) lolButton.setText("You lol this. (" + lolCount + ")");
        else lolButton.setText("LOL (" + lolCount + ")");
    }

    private void updateBooButtonText(boolean booed, int booCount)
    {
        if(booed) booButton.setText("You boo this. (" + booCount + ")");
        else booButton.setText("BOO (" + booCount + ")");
    }

    private void changeLayoutAccordingToMediaType(){
        if(mediaType == MediaType.NONE)
        {
            audioHolder.setVisibility(View.GONE);
            iw.setVisibility(View.GONE);
            videoHolder.setVisibility(View.GONE);
        }
        else if(mediaType == MediaType.AUDIO)
        {
            iw.setVisibility(View.GONE);
            videoHolder.setVisibility(View.GONE);
            audioHolder.setVisibility(View.GONE);
        }
        else if(mediaType == MediaType.PIC)
        {
            audioHolder.setVisibility(View.GONE);
            videoHolder.setVisibility(View.GONE);
            Picasso.with(this.getActivity()).load(this.mediaFile.getUrl()).into(this.iw);

            /*Bitmap bm = BitmapFactory.decodeFile(picPath);
            iw.setImageBitmap(bm);
            focused_iw = (ImageView)view.findViewById(R.id.focused_pic);
            focused_iw.setImageBitmap(bm);*/

            ImageView focused_imageView = (ImageView)this.view.findViewById(R.id.focused_pic);
            Picasso.with(this.getActivity()).load(this.mediaFile.getUrl()).into(focused_imageView);

        }
        else
        {
            audioHolder.setVisibility(View.GONE);
            iw.setVisibility(View.GONE);
        }


        if(this.mediaType != MediaType.NONE && this.mediaType != MediaType.PIC){
            System.gc();

            this.mediaFile.getDataInBackground(new GetDataCallback() {
                @Override
                public void done(byte[] bytes, ParseException e) {
                    if(!FlagFragment.this.isAdded()){
                        //orrible hack to prevent a crash when the Fragment is detached from an activity.
                        return;
                    }
                    if(e == null){
                        try {

                            MediaType mediaType = FlagFragment.this.mediaType;
                            File tempFile = FlagFragment.this.tempFileForMediaType(mediaType);
                            FileOutputStream outputStream = new FileOutputStream(tempFile);
                            outputStream.write(bytes);
                            outputStream.flush();
                            outputStream.close();

                            if(mediaType == MediaType.AUDIO){
                                FlagFragment.this.onAudioDownloaded(tempFile.getAbsolutePath());
                            }
                            else if(mediaType == MediaType.VIDEO){
                                FlagFragment.this.onVideoDownloaded(tempFile.getAbsolutePath());
                            }
                            FlagFragment.this.mediaFile = null;

                        }
                        catch(IOException io){
                            Log.d(TAG, "IO Error", io);
                            Toast.makeText(FlagFragment.this.getActivity(), "Error downloading file", Toast.LENGTH_LONG).show();
                        }
                    }
                    else{
                        Log.d(TAG, "Download Error", e);
                        Toast.makeText(FlagFragment.this.getActivity(), "Error downloading file", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }


    }

    private  File tempFileForMediaType(MediaType type){
        String fileName;
        String fileFormat;
        switch (type){
            case AUDIO:
                fileFormat = ShareActivity.AUDIO_FORMAT;
                fileName = "places_temp_audio";
                break;
            case VIDEO:
                fileFormat = ShareActivity.VIDEO_FORMAT;
                fileName = "places_temp_video";
                break;
            case PIC:
                fileFormat = ShareActivity.PICTURE_FORMAT;
                fileName = "places_temp_pic";
                break;
            default: return null;
        }

        try{
            Log.d(TAG, "name "+fileName);
            Log.d(TAG, "format "+fileFormat);
            return File.createTempFile( fileName, fileFormat, this.getActivity().getCacheDir());
        }
        catch(IOException e){
            Log.d(TAG, "Cannot create temp file", e);
            return null;
        }

    }

}

