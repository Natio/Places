package com.gcw.sapienza.places;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.gcw.sapienza.places.utils.FacebookUtilCallback;
import com.gcw.sapienza.places.utils.FacebookUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Created by mic_head on 02/02/15.
 */
public class FlagFragment extends Fragment implements View.OnClickListener, View.OnTouchListener {

    private String text;
    private String id;
    private String date;
    private String weather;
    private String category;

    private String pic_path;
    private String audio_path;
    private String video_path;

    private MediaPlayer mediaPlayer;
    private VideoView vv;
    private ImageView iw;
    private TextView authorTextView;
    private EditText flagText;
    private ImageView profilePicimageView;
    private RelativeLayout frameLayout;
    private ImageView focused_iw;
    private ImageView playVideoButton;
    private FrameLayout videoHolder;
    private ImageView audioHolder;

    private enum MediaType{ PIC, AUDIO, VIDEO, NONE }
    private MediaType mediaType;

    private View view;

    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = "FlagFragment";

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

        pic_path = bundle.getString("picture");
        if(pic_path != null )
        {
            mediaType = MediaType.PIC;
            return;
        }

        video_path = bundle.getString("video");
        if(video_path != null )
        {
            mediaType = MediaType.VIDEO;
            return;
        }

        audio_path = bundle.getString("audio");
        if(audio_path != null )
        {
            mediaType = MediaType.AUDIO;
            return;
        }

        mediaType = MediaType.NONE;
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
        flagText = (EditText)view.findViewById(R.id.text);
        authorTextView = (TextView)view.findViewById(R.id.author);
        profilePicimageView = (ImageView)view.findViewById(R.id.profile_pic);
        frameLayout = (RelativeLayout)view.findViewById(R.id.frame_layout);
        playVideoButton = (ImageView)view.findViewById(R.id.play_video_button);
        videoHolder = (FrameLayout)view.findViewById(R.id.video_holder);
        audioHolder = (ImageView) view.findViewById(R.id.audio);

        iw.setOnClickListener(this);
        vv.setOnTouchListener(this);
        audioHolder.setOnClickListener(this);
        // playVideoButton.setOnClickListener(this);

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

            try {
                File temp = new File(audio_path);

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
        else if(mediaType == MediaType.PIC)
        {
            Bitmap bm = BitmapFactory.decodeFile(this.pic_path);

            audioHolder.setVisibility(View.GONE);
            videoHolder.setVisibility(View.GONE);
            iw.setImageBitmap(bm);

            focused_iw = (ImageView)view.findViewById(R.id.focused_pic);
            focused_iw.setImageBitmap(bm);
        }
        else
        {
            audioHolder.setVisibility(View.GONE);
            iw.setVisibility(View.GONE);

            Uri videoUri = Uri.parse(video_path);
            vv.setVideoURI(videoUri);
            playVideoButton.setVisibility(View.VISIBLE);
            // playVideo();

            vv.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    playVideoButton.setVisibility(View.VISIBLE);
                }
            });
        }

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
        if(v.getId() == vv.getId() && event.getAction() == MotionEvent.ACTION_DOWN) return playVideo();

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

    @Deprecated
    private void loadProfilePictureFromUrl(String url){
        Picasso.with(getActivity().getApplicationContext()).load(url).into((ImageView)view.findViewById(R.id.profile_pic));
    }

    @Deprecated //use loadProfilePictureFromUrl
    protected void streamProfilePic(final String image_url){
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    final Bitmap bitmap = BitmapFactory.decodeStream(new URL(image_url).openConnection().getInputStream());

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((ImageView) view.findViewById(R.id.profile_pic)).setImageBitmap(bitmap);
                        }
                    });
                }
                catch (IOException ioe){ ioe.printStackTrace(); }
            }
        }).start();
    }
}

