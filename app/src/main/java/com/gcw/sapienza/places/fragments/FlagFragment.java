package com.gcw.sapienza.places.fragments;


import android.app.DownloadManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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
import com.gcw.sapienza.places.model.CustomUser;
import com.gcw.sapienza.places.model.Flag;
import com.gcw.sapienza.places.utils.FacebookUtilCallback;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
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
    private ArrayList<String> wowIds;

    private MediaPlayer mediaPlayer;
    private VideoView vv;
    private ImageView iw;
    private TextView authorTextView;
    private RelativeLayout frameLayout;
    private ImageView playVideoButton;
    private FrameLayout videoHolder;
    private ImageView audioHolder;
    private Button wowButton;

    public static enum MediaType{ PIC, AUDIO, VIDEO, NONE }
    private MediaType mediaType;
    private ParseFile mediaFile;

    private View view;

    private static final String TAG = "FlagFragment";

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
        wowIds = bundle.getStringArrayList("wowIds");
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

        iw.setOnClickListener(this);
        vv.setOnTouchListener(this);
        audioHolder.setOnClickListener(this);
        frameLayout.setOnClickListener(this);
        wowButton.setOnClickListener(this);
        // playVideoButton.setOnClickListener(this);

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

        boolean wowed = true;
        int wowCount = 0;

        if(wowIds != null)
        {
            wowCount = wowIds.size();

            for (int i = 0; i < wowCount; i++)
                if (wowIds.get(i).equals(FacebookUtils.getInstance().getCurrentUserId())) wowed = true;
        }

        updateWowButtonText(wowed, wowCount);
    }

    @Override
    public void onClick(View v)
    {
        if(v.getId() == R.id.frame_layout) frameLayout.setVisibility(View.GONE);
        else if(v.getId() == R.id.pic) frameLayout.setVisibility(View.VISIBLE);
        // else if(v.getId() == playVideoButton.getId()) playVideo();
        else if(v.getId() == R.id.audio) playRecording();
        else if(v.getId() == R.id.wow_button) wowFlag();
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

    private void wowFlag()
    {
        final String userId = FacebookUtils.getInstance().getCurrentUserId();

        ParseQuery<Flag> queryPosts = ParseQuery.getQuery("Posts");
        queryPosts.whereEqualTo("objectId", flagId);

        ParseQuery<CustomUser> queryUser = ParseQuery.getQuery("Wow_Lol_Boo");
        queryUser.whereEqualTo("fbId", userId);

        wowButton.setClickable(false);

        queryPosts.findInBackground(new FindCallback<Flag>()
        {
            public void done(List<Flag> markers, ParseException e)
            {
                if (e == null && markers.size() != 0)
                {
                    Flag flag = markers.get(0);

                    ArrayList<String> wowIds = new ArrayList<String>();
                    wowIds = flag.getWowIds();

                    final int wowCount = wowIds.size();

                    if(!wowIds.contains(userId))
                    {
                        flag.addWowId(userId);

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
                        flag.deleteWowId(userId);

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

        queryUser.findInBackground(new FindCallback<CustomUser>()
        {
            public void done(List<CustomUser> markers, ParseException e)
            {
                if (e == null && markers.size() != 0)
                {
                    CustomUser user = markers.get(0);

                    ArrayList<String> wows = new ArrayList<String>();
                    wows = user.getWows();

                    if(!wows.contains(flagId))
                    {
                        user.addWow(flagId);
                        user.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e)
                            {
                                wowButton.setClickable(true);
                            }
                        });
                    }
                    else
                    {
                        user.deleteWow(flagId);
                        user.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e)
                            {
                                wowButton.setClickable(true);
                            }
                        });
                    }
                }
                else if (markers.size() == 0)
                {
                    CustomUser user = new CustomUser();
                    user.addWow(flagId);
                    user.setFacebookId(FacebookUtils.getInstance().getCurrentUserId());
                    user.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e)
                        {
                            wowButton.setClickable(true);
                        }
                    });
                }
                else
                {
                    Toast.makeText(getActivity(), "Error encounterd while accessing database", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Error encounterd while retrieving table entry on Parse.com");

                    wowButton.setClickable(true);
                }
            }
        });
        /*
        else
        {
            queryPosts.findInBackground(new FindCallback<Flag>()
            {
                public void done(List<Flag> markers, ParseException e)
                {
                    if (e == null && markers.size() != 0)
                    {
                        Flag flag = markers.get(0);
                        flag.deleteWowId(FacebookUtils.getInstance().getCurrentUserId());
                        flag.saveInBackground(new SaveCallback()
                        {
                            @Override
                            public void done(ParseException e)
                            {
                                wowed=false;
                                wowCount--;
                                updateWowButtonText();
                            }
                        });
                    } else
                    {
                        Toast.makeText(getActivity(), "Error encounterd while accessing database", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Error encounterd while retrieving table entry on Parse.com");
                    }

                    wowButton.setClickable(true);
                }
            });

            queryUser.findInBackground(new FindCallback<CustomUser>()
            {
                public void done(List<CustomUser> markers, ParseException e)
                {
                    if (e == null && markers.size() != 0)
                    {
                        CustomUser user = markers.get(0);
                        user.deleteWow(flagId);
                        user.saveInBackground();
                    }
                    else
                    {
                        Toast.makeText(getActivity(), "Error encounterd while accessing database", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Error encounterd while retrieving table entry on Parse.com");
                    }

                    wowButton.setClickable(true);
                }
            });
        }*/
    }

    private void updateWowButtonText(boolean wowed, int wowCount)
    {
        if(wowed) wowButton.setText("You wow this. (" + wowCount + ")");
        else wowButton.setText("WOW (" + wowCount + ")");
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

