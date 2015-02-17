package com.gcw.sapienza.places;


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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.gcw.sapienza.places.utils.FacebookUtilCallback;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by mic_head on 02/02/15.
 */
public class FlagFragment extends Fragment implements View.OnClickListener, View.OnTouchListener {

    private String text;
    private String id;
    private String date;
    private String weather;
    private String category;


    private MediaPlayer mediaPlayer;
    private VideoView vv;
    private ImageView iw;
    private TextView authorTextView;
    private RelativeLayout frameLayout;
    private ImageView playVideoButton;
    private FrameLayout videoHolder;
    private ImageView audioHolder;

    public static enum MediaType{ PIC, AUDIO, VIDEO, NONE }
    private MediaType mediaType;
    private ParseFile mediaFile;

    private View view;

    @SuppressWarnings("UnusedDeclaration")
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

        iw.setOnClickListener(this);
        vv.setOnTouchListener(this);
        audioHolder.setOnClickListener(this);
        frameLayout.setOnClickListener(this);
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
                if (keyCode == KeyEvent.KEYCODE_BACK && frameLayout.getVisibility() == View.VISIBLE)
                {
                    frameLayout.setVisibility(View.GONE);

                    return true;
                }
                return false;
            }
        });

        return view;
    }

    @Override
    public void onClick(View v)
    {
        if(v.getId() == R.id.frame_layout) frameLayout.setVisibility(View.GONE);
        else if(v.getId() == R.id.pic) frameLayout.setVisibility(View.VISIBLE);
        // else if(v.getId() == playVideoButton.getId()) playVideo();
        else if(v.getId() == R.id.audio) playRecording();
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

